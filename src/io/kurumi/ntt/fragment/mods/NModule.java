package io.kurumi.ntt.fragment.mods;

import java.util.List;
import org.mozilla.javascript.Scriptable;
import java.io.File;

public class NModule {

	public String id;
	public String name;
	public String version;
	public Integer versionCode;
	public String author;
	public List<String> dependencies;
	public List<String> libs;
	public String main;
	public List<String> cmds;
	public List<String> actions;
	
	public transient File modPath;
	public transient Scriptable env;
	public transient ModuleException error;
	
	public String format() {
		
		return "[ " + name + " - " + version + " ]";
		
	}
	
}
