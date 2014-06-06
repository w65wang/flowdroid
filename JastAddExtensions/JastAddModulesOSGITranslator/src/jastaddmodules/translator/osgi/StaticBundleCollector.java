package jastaddmodules.translator.osgi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;

import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.osgi.framework.BundleException;

public class StaticBundleCollector {
	StaticBundleEnvironment environment;
	//absolute file path to bundle mapping
	HashMap<String, BundleDescription> fileToBundleMap = new HashMap<String, BundleDescription>();
	
	public StaticBundleCollector(StaticBundleEnvironment environment) {
		this.environment = environment;
	}
	
	public StaticBundleCollector(String[] files, StaticBundleEnvironment environment) throws BundleException, IOException {
		this.environment = environment;
		for (String file: files) {
			addBundleFile(file);
		}
	}

	public void addBundleFile(File file) throws BundleException, IOException {
		StateObjectFactory bundleDescriptorFactory = StateObjectFactory.defaultFactory;
		Dictionary manifestDictionary = Headers
				.parseManifest(new FileInputStream(file));
		BundleDescription bundleDesc = bundleDescriptorFactory
				.createBundleDescription(null, manifestDictionary,
						file.getAbsolutePath(), 0);
		environment.addBundle(bundleDesc);
		fileToBundleMap.put(file.getAbsolutePath(), bundleDesc);
	}
	
	public void addBundleFile(String file) throws BundleException, IOException {
		File manifestFile = new File(file);
		addBundleFile(manifestFile);
	}

	public StaticBundleEnvironment getBundleEnvironment() {
		return environment;
	}
	
	public BundleDescription getBundleFromFile(File file) {
		return fileToBundleMap.get(file.getAbsolutePath());
	}
}
