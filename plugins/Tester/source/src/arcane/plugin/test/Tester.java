package arcane.plugin.test;

import arcane.ArcaneException;
import arcane.Plugin;
import arcane.ui.util.ProgressDialog;

public class Tester extends Plugin{

	@Override
	public String getName() {
		return "Tester";
	}

	@Override
	public void install(ProgressDialog dialog) {
		// TODO Auto-generated method stub
		throw new ArcaneException("Tester Exception!");
	}

}
