package jastaddmodules.translator.oomodules;

import java.util.Collection;

public class WeakModuleInterface extends ModuleInterface {
	
	public WeakModuleInterface(String name) {
		super(name, null);
	}
	
	public WeakModuleInterface(String name, 
			ModuleInterface superModule, 
			Collection<String> exportedPackages) {
		super(name, superModule, exportedPackages);
	}
	
	@Override
	protected String getModuleKeyword() {
		return "weak_module_interface";
	}
	
	@Override
	public boolean implementedBy(AbstractModule module) {
		for (String exportedPackage : this.exportedPackages) {
			if (!module.getExportedPackages().contains(exportedPackage)) {
				return false;
			}
		}
		return true;
	}
}
