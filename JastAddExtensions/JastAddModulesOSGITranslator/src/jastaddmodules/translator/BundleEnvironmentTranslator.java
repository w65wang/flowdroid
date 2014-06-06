package jastaddmodules.translator;

import jastaddmodules.translator.oomodules.AbstractModule;
import jastaddmodules.translator.oomodules.ConcreteModule;
import jastaddmodules.translator.oomodules.ModuleImport;
import jastaddmodules.translator.oomodules.ModuleInterface;
import jastaddmodules.translator.oomodules.ModuleReference;
import jastaddmodules.translator.oomodules.WeakModuleInterface;
import jastaddmodules.translator.osgi.BundleBucket;
import jastaddmodules.translator.osgi.StaticBundleEnvironment;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class BundleEnvironmentTranslator {
	// Passes:
	// generateOOModules -> creates an ConcreteModule instance for each
	// 		BundleInstance (includes exported packages)
	// generateOverrides -> generates the overrides relations from the versions.
	// 		implicit assumption that a higher version overrides a lower version
	// generateRBInterfaces -> creates ModuleInterfaces derived from the
	// 		required bundle constraints, and adds these as imports and
	// 		implements in the appropriate modules
	// generateIPInterfaces -> creates WeakModuleInterfaces derived from the
	// 		import package declarations
	// generateSystemModule -> generates the top level system module, which does
	// 		the wiring using replace declarations to fill in the interface references 
	//		with an actual module. Should take into consideration the singleton 
	//		attribute of bundles

	StaticBundleEnvironment bundleEnv;
	HashMap<BundleDescription, AbstractModule> bundleMap = new HashMap<BundleDescription, AbstractModule>();
	HashMap<String, ModuleInterface> rbInterfaceMap = new HashMap<String, ModuleInterface>();
	HashMap<String, WeakModuleInterface> ipInterfaceMap = new HashMap<String, WeakModuleInterface>();
	ConcreteModule systemModule; // filled in by the last pass, contains the

	// replace declarations that links the
	// interfaces to the

	public BundleEnvironmentTranslator(StaticBundleEnvironment bundleEnv) {
		this.bundleEnv = bundleEnv;
	}

	public List<AbstractModule> translate(String destdir) throws IOException, BundleTranslationException {
		LinkedList<AbstractModule> ret = new LinkedList<AbstractModule>();
		generateOOModules();
		generateOverrides();
		generateRBInterfaces();
		generateIPInterfaces();
		generateSystemModule();

		ret.addAll(bundleMap.values());
		ret.addAll(rbInterfaceMap.values());
		ret.addAll(ipInterfaceMap.values());
		ret.add(systemModule);
		
		return ret;
	}



	// PASS---------------------------------------------------------
	protected void generateOOModules() {
		for (BundleDescription bundle : bundleEnv.getAllBundles()) {
			assert (bundleMap.get(bundle) == null) : "Entry already present for "
					+ bundle;
			AbstractModule module = createOOModuleFromBundle(bundle);
			// exported packages
			for (ExportPackageDescription epd : bundle.getExportPackages()) {
				module.addExportedPackage(epd.getName());
			}

			bundleMap.put(bundle, module);
		}
	}

	protected AbstractModule createOOModuleFromBundle(BundleDescription bundle) {
		return new ConcreteModule(makeBundleOOName(bundle), bundle);
	}

	private String makeBundleOOName(BundleDescription bundle) {
		return bundle.getName() + "_" + getVersionString(bundle.getVersion());
	}

	// PASS---------------------------------------------------------
	protected void generateOverrides() {
		for (BundleBucket bucket : bundleEnv.getAllBundleBuckets()) {
			List<AbstractModule> prevModules = new LinkedList<AbstractModule>();
			for (Iterator<BundleDescription> iter = bucket.getAllBundles()
					.iterator(); iter.hasNext();) {
				BundleDescription bundle = iter.next();
				AbstractModule module = bundleMap.get(bundle);
				assert (module != null) : "Not found in bundle map: " + bundle;

				module.addOverridenModules(prevModules);
				prevModules.add(module);
			}
		}
	}

	// PASS---------------------------------------------------------
	protected void generateRBInterfaces() {
		for (BundleDescription bundle : bundleEnv.getAllBundles()) {
			AbstractModule module = bundleMap.get(bundle);
			assert (module != null) : "No matching module for bundle " + bundle;
			for (BundleSpecification reqSpec : bundle.getRequiredBundles()) {
				ModuleInterface rbInterface = makeNewRBInterface(reqSpec);
				module.addImportedModule(rbInterface, rbInterface.getName());
			}
		}
	}

	protected ModuleInterface getRBInterface(String interfaceName) {
		ModuleInterface ret = rbInterfaceMap.get(interfaceName);
		return ret;
	}

	protected ModuleInterface makeNewRBInterface(BundleSpecification reqSpec) {
		String symbolicName = reqSpec.getName();
		VersionRange range = reqSpec.getVersionRange();
		String interfaceName = makeRBInterfaceName(symbolicName, range);
		// if already created, return the existing interface
		if (getRBInterface(interfaceName) != null) {
			return getRBInterface(interfaceName);
		}
		ModuleInterface ret = new ModuleInterface(interfaceName, reqSpec);
		rbInterfaceMap.put(interfaceName, ret);

		// create the exported packages from the intersection of the packages
		// that are
		// exported by the visible bundles that are available. Also, add the
		// interface
		// to the implements list of the matching modules
		boolean first = true;
		for (BundleDescription bundle : bundleEnv.getBundles(symbolicName,
				range)) {
			// export package conjunction
			if (first) {
				ret.addExportedPackages(getExportedPackages(bundle));
			} else {
				ret.getExportedPackages()
						.retainAll(getExportedPackages(bundle));
			}
			// interface implements
			AbstractModule module = bundleMap.get(bundle);
			assert (module != null) : "Module not found for bundle " + bundle;
			module.addImplementedInterface(ret);
			first = false;
		}
		return ret;
	}

	protected String makeRBInterfaceName(String symbolicName, VersionRange range) {
		String ret = "";

		// return is I_internal_symbolicName_v__minver__maxver. The __ before
		// minver and maxver is doubled
		// if they are exclusive i.e., com.xyz (1.6,1.7] is encoded as
		// com.xyz_v____1_6__1_7
		ret += "I_internal_" + symbolicName + "_v";
		if (range.getIncludeMinimum()) {
			ret += "__";
		} else {
			ret += "____";
		}
		ret += getVersionString(range.getMinimum());
		if (range.getIncludeMaximum()) {
			ret += "__";
		} else {
			ret += "____";
		}
		ret += getVersionString(range.getMaximum());

		return ret;
	}

	// PASS---------------------------------------------------------
	protected void generateIPInterfaces() {
		int counter = 0;
		for (BundleDescription bundle : bundleEnv.getAllBundles()) {
			AbstractModule module = bundleMap.get(bundle);
			assert (module != null) : "Module not found for bundle " + bundle;
			if (bundle.getImportPackages().length > 0) {
				Collection<Collection<String>> partitions = partitionImportedPackages(getImportedPackages(bundle));
				for (Collection<String> partition : partitions) {
					ModuleInterface newInterface = makeIPInterface(bundle,
							partition, counter);
					module.addImportedModule(newInterface, newInterface
							.getName());
					counter++;
				}
			}
		}
	}

	// create a weak module interface, which exports the packages that the
	// context imports
	protected WeakModuleInterface makeIPInterface(BundleDescription context,
			Collection<String> importedPackages, int counter) {
		String interfaceName = makeIPInterfaceName(context, counter);
		assert (ipInterfaceMap.get(interfaceName) == null) : "Weak interface of the same name already exists: "
				+ interfaceName;
		WeakModuleInterface ret = new WeakModuleInterface(interfaceName);

		ret.addExportedPackages(importedPackages);
		ipInterfaceMap.put(ret.getName(), ret);
		return ret;
	}

	protected String makeIPInterfaceName(BundleDescription context, int counter) {
		String ret = "";

		ret += "WI_importpackage_" + context.getSymbolicName() + "_v_"
				+ getVersionString(context.getVersion()) + "_ctr_" + counter;

		return ret;
	}

	// PASS---------------------------------------------------------
	protected void generateSystemModule() throws BundleTranslationException {
		// TODO: Implement
		this.systemModule = new ConcreteModule("system", null);
		// import an instance of every concrete module
		for (AbstractModule module : bundleMap.values()) {
			this.systemModule.addImportedModule(module, module.getName());
		}
		// wiring, replace every import in the concrete modules with the
		// resolved
		// module

		for (AbstractModule module : bundleMap.values()) {
			for (ModuleImport moduleImport : module.getImportedModules()) {
				// TODO: refactor to remove instanceof
				// replace ordinary interfaces introduced by require bundle
				if (moduleImport.getImportedModule() instanceof ModuleInterface
						&& !(moduleImport.getImportedModule() instanceof WeakModuleInterface)) {
					// resolve the import
					ModuleInterface replaceTarget = (ModuleInterface) moduleImport
							.getImportedModule();
					BundleDescription resolvedRequire = bundleEnv
							.resolve(replaceTarget.getSrcRequire());
					if (resolvedRequire == null) {
						throw new BundleTranslationException(
								"Unable to resolve required bundle "
										+ replaceTarget.getSrcRequire()
										+ " in bundle "
										+ ((ConcreteModule) module)
												.getSrcBundle());
					}
					AbstractModule replacementModule = bundleMap
							.get(resolvedRequire);
					assert (replacementModule != null) : "Unable to find module for bundle "
							+ resolvedRequire;

					// add the replace statement in the system module
					systemModule.addReplace(module.getName()
							+ ModuleReference.MODULE_SEPARATOR
							+ replaceTarget.getName(), replacementModule
							.getName());
				}
				// replace ordinary interfaces introduced by require bundle
				else if (moduleImport.getImportedModule() instanceof WeakModuleInterface) {
					WeakModuleInterface replaceTarget = (WeakModuleInterface) moduleImport
							.getImportedModule();
					BundleDescription resolvedIP = resolveImplementingBundle(replaceTarget);
					if (resolvedIP == null) {
						String packages = "";
						for (String packageName : replaceTarget.getExportedPackages()) {
							packages += packageName + " ";
						}
						throw new BundleTranslationException(
								"Unable to wire bundle "
										+ ((ConcreteModule) module)
												.getSrcBundle()
										+ " imported packages " + packages);
					}
					AbstractModule replacementModule = bundleMap
							.get(resolvedIP);
					assert (replacementModule != null) : "Unable to find module for bundle "
							+ resolvedIP;

					// add the replace to the system module
					systemModule.addReplace(module.getName()
							+ ModuleReference.MODULE_SEPARATOR
							+ replaceTarget.getName(), replacementModule
							.getName());

				}
			}
		}

	}

	private BundleDescription resolveImplementingBundle(
			WeakModuleInterface weakInterface) {
		Collection<BundleDescription> ret = new LinkedList<BundleDescription>();

		// go through non-singleton bundles first, if one matches the interface,
		// resolve it and return it
		for (BundleDescription bundle : bundleEnv.getAllBundles()) {
			if (bundle.isSingleton()) {
				continue;
			}
			AbstractModule module = bundleMap.get(bundle);
			assert (module != null) : "No module found for bundle " + bundle;
			if (weakInterface.implementedBy(module)) {
				BundleDescription resolvedBundle = bundleEnv.resolve(bundle
						.getSymbolicName(), new VersionRange(bundle
						.getVersion(), true, bundle.getVersion(), true));
				if (resolvedBundle != null) {
					return resolvedBundle;
				}
			}
		} 

		// then go through the singleton bundles, resolving every bundle until
		// a non-null return
		for (BundleDescription bundle : bundleEnv.getAllBundles()) {
			if (!bundle.isSingleton()) {
				continue;
			}
			AbstractModule module = bundleMap.get(bundle);
			assert (module != null) : "No module found for bundle " + bundle;
			if (weakInterface.implementedBy(module)) {
				BundleDescription resolvedBundle = bundleEnv.resolve(bundle
						.getSymbolicName(), new VersionRange(bundle
						.getVersion(), true, bundle.getVersion(), true));
				if (resolvedBundle != null) {
					return resolvedBundle;
				}
			}
		}

		return null;
	}

	// Util methods
	private Collection<String> getExportedPackages(BundleDescription bundle) {
		Collection<String> ret = new LinkedList<String>();
		for (ExportPackageDescription packageDesc : bundle.getExportPackages()) {
			ret.add(packageDesc.getName());
		}
		return ret;
	}

	private Collection<String> getImportedPackages(BundleDescription bundle) {
		Collection<String> ret = new LinkedList<String>();
		for (ImportPackageSpecification packageDesc : bundle
				.getImportPackages()) {
			ret.add(packageDesc.getName());
		}
		return ret;
	}

	private String getVersionString(Version version) {
		return version.toString().replace('.', '_');
	}

	// uses a heuristic to partition the set of imported packages. Uses the
	// first three
	// segments of the package name to group packages into separate weak
	// interface
	// groups. Is not perfect, but would be enough for the case study
	private Collection<Collection<String>> partitionImportedPackages(
			Collection<String> importedPackages) {
		HashMap<String, Collection<String>> partitionMap = new HashMap<String, Collection<String>>();
		for (String packageName : importedPackages) {
			String partName = getFirstPackageSegments(packageName);
			Collection<String> partition = partitionMap.get(partName);
			if (partition == null) {
				partition = new HashSet<String>();
				partitionMap.put(partName, partition);
			}
			partition.add(packageName);
		}
		return partitionMap.values();
	}

	public String getFirstPackageSegments(String packageName) {
		String[] segments = packageName.split("\\.");
		if (segments.length >= 3) {
			return segments[0] + "." + segments[1] + "." + segments[2];
		} else {
			return packageName;
		}
	}
	
	public AbstractModule getModuleFromBundle(BundleDescription bundleDesc) { 
		return bundleMap.get(bundleDesc);
	}
}
