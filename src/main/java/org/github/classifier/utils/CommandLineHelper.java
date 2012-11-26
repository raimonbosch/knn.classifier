package org.github.classifier.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class CommandLineHelper {

	private String []args;

	private Options options;
    private CommandLineParser parser = null;
    private CommandLine cmd ;
	
	public CommandLineHelper(String[] args) {
		super();
		this.args = args;
		
		options = new Options();
	}

	private Option buildSimpleOption(String opt, boolean hasArgs, String description ){
		Option option = new Option(opt, hasArgs, description);
		return option;
	}
	
	private Option buildOption (String opt, String longOpt, boolean hasArgs, String description  ){
		Option option = new Option(opt, longOpt, hasArgs, description);
		return option;
	}
	
	/**
	 * The options set through this method will be handled as required by the system.
	 * In case this option does not exist, while parsing, an exception (ParseException) will
	 * be raised.
	 * @param opt
	 * @param longOpt
	 * @param hasArgs
	 * @param description
	 * @param sep
	 */
	public void setRequiredOption( String opt, String longOpt, boolean hasArgs, String description , char sep ){
		Option option = buildOption(opt, longOpt, hasArgs, description);
		
		option.setRequired(true);
		option.setValueSeparator(sep);
		
		options.addOption(option);
	}
	
	public void setRequiredOption( String opt, boolean hasArgs, String description , char sep ){
		Option option = buildSimpleOption(opt, hasArgs, description);
		
		option.setRequired(true);
		option.setValueSeparator(sep);
		
		options.addOption(option);
	}
	
	public void setRequiredOption( String opt, boolean hasArgs, String description ){
		this.setRequiredOption(opt, hasArgs, description, ' ');
	}
	
	/**
	 * Sets the mandatory option using the default separator ' ' (space).
	 * @param opt
	 * @param longOpt
	 * @param hasArgs
	 * @param description
	 */
	public void setRequiredOption( String opt, String longOpt, boolean hasArgs, String description ){
		this.setRequiredOption(opt, longOpt, hasArgs, description, ' ');
		
	}
	
	public void setOption(String opt, boolean hasArgs, String description){
		this.setOption(opt, hasArgs, description, ' ');
	}
	
	/**
	 * Setting option with default separator.
	 * @param opt
	 * @param longOpt
	 * @param hasArgs
	 * @param description
	 */
	public void setOption(String opt, String longOpt, boolean hasArgs, String description){
		this.setOption(opt, longOpt, hasArgs, description, ' ');
	}
	
	public void setOption(String opt, boolean hasArgs, String description, char sep  ){
		Option option = buildSimpleOption(opt, hasArgs, description);
		option.setValueSeparator(sep);
		options.addOption(option);
	}
	
	public void setOption(String opt, String longOpt, boolean hasArgs, String description, char sep  ){
		Option option = buildOption(opt, longOpt, hasArgs, description);
		option.setValueSeparator(sep);
		options.addOption(option);
	}

	
	
	public CommandLine parsePosix() throws ParseException{
		if (parser == null){
			parser = new PosixParser();
			cmd = parser.parse(options, args);
		}
		return cmd ;
	}
	
	
	public boolean hasOption(String opt) throws ParseException{
		if ( cmd == null)
			parsePosix();
		return cmd.hasOption(opt);
	}
	
	public String getOptionValue(String opt) throws ParseException{
		if ( cmd == null)
			parsePosix();
		return cmd.getOptionValue(opt);
	}
	
	public void printHelp(Class clazz){
		new HelpFormatter().printHelp(clazz.getCanonicalName(), options);
	}
	
}
