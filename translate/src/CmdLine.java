/**
 * @author Vity
 */

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.DisplaySetting;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.commons.cli2.option.DefaultOption;
import org.apache.commons.cli2.option.PropertyOption;
import org.apache.commons.cli2.util.HelpFormatter;
import utilities.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * @author Vity
 */
final class CmdLine {
    private final MainApp app;
    private Map<String, String> properties = new HashMap<String, String>(2);
    private boolean resetOptions;
    private boolean minimize;
    private boolean nosplash;

    CmdLine(MainApp app) {
        this.app = app;
        resetOptions = false;
        minimize = false;
        nosplash = false;
    }

    private void showVersion() {
        System.out.println("1.0");
        System.out.println("Vity");
        app.exit();
    }


    @SuppressWarnings({"unchecked"})
    public List<String> processCommandLine(String[] args) {
        final String appPath = Utils.getAppPath();
        final File startup = new File(appPath, "startup.properties");
        if (startup.exists() && startup.isFile() && startup.canRead()) {
            Scanner scanner = null;
            final List<String> list = new LinkedList<String>();
            try {
                scanner = new Scanner(startup);
                while (scanner.hasNextLine()) {
                    final String line = scanner.nextLine().trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        list.add(line);
                    }
                }
                if (!list.isEmpty()) {
                    list.addAll(Arrays.asList(args));
                    args = new String[list.size()];
                    list.toArray(args);
                }
            }
            catch (FileNotFoundException e) {
                //ignore
            } finally {
                if (scanner != null)
                    scanner.close();
            }
        }
        if (args.length == 0)
            return new LinkedList<String>();

        final DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
        //new ArgumentBuilder();
        final GroupBuilder gbuilder = new GroupBuilder();


        final DefaultOption helpOption = obuilder.withShortName("h").withShortName("?").withLongName("help").withDescription("print this message").create();
        final DefaultOption versionOption = obuilder.withShortName("v").withLongName("version").withDescription("print the version information and exit").create();
        final DefaultOption browserOption = obuilder.withShortName("b").withLongName("nobrowser").withDescription("does not open browser").create();

        final PropertyOption propertyOption = new PropertyOption();

        //propertyOption.getDescription()

//        obuilder.withChildren()
//        Option property  = OptionBuilder.withArgName( "property=value" )
//                                .hasArgs()
//                                .withValueSeparator()
//                                .withDescription( "use value for given property" )
//                                .create( "D" );

//        final Argument argument = new ArgumentBuilder().withName("property").withSubsequentSeparator(',').create();

//        final FileValidator fileValidator = new FileValidator();
//        fileValidator.setExisting(true);
//        fileValidator.setDirectory(false);
//        Argument fileArg = abuilder.withName("file").withMinimum(1).withValidator(fileValidator).create();
//        DefaultOption fileOption = obuilder.withRequired(false).withDescription("files to open").withLongName("open").withShortName("o").withArgument(fileArg).create();

        Group options = gbuilder
                .withName("options")
                .withOption(helpOption)
                .withOption(versionOption)
                .withOption(propertyOption)
                .withOption(browserOption)
                .create();
        Parser parser = new Parser();
        parser.setGroup(options);
        try {
            CommandLine cmd = parser.parse(args);

            if (cmd.hasOption(helpOption)) {
                printHelp(options);
            } else if (cmd.hasOption(versionOption)) {
                showVersion();
            }
            if (cmd.hasOption(browserOption)) {
                this.nosplash = true;
            }
            final Set<String> set = (Set<String>) cmd.getProperties(propertyOption);

            for (String o : set) {
                properties.put(o, cmd.getProperty(propertyOption, o, ""));
            }

//            } else if (cmd.hasOption(fileOption)) {
//                return cmd.getValues(fileOption);
//            }
        } catch (OptionException e) {
            e.printStackTrace();
            printHelp(options);
            System.exit(-1);

        }
        return new LinkedList<String>();
    }

    @SuppressWarnings({"unchecked"})
    private void printHelp(Group options) {
        HelpFormatter f = new HelpFormatter();
        f.getDisplaySettings().add(DisplaySetting.DISPLAY_PROPERTY_OPTION);
        f.setGroup(options);
        f.setShellCommand("translate");
        //       f.getFullUsageSettings().add(DisplaySetting.ALL);
//        f.getFullUsageSettings().add(DisplaySetting.DISPLAY_GROUP_NAME);
//        f.getFullUsageSettings().add(DisplaySetting.DISPLAY_GROUP_ARGUMENT);
//        f.getFullUsageSettings().remove(DisplaySetting.DISPLAY_GROUP_EXPANDED);
//        f.getLineUsageSettings().add(DisplaySetting.ALL);
//        f.getLineUsageSettings().add(DisplaySetting.DISPLAY_PARENT_ARGUMENT);
//        f.getLineUsageSettings().add(DisplaySetting.DISPLAY_ARGUMENT_BRACKETED);
        f.getLineUsageSettings().add(DisplaySetting.DISPLAY_PROPERTY_OPTION);
//        f.getLineUsageSettings().add(DisplaySetting.DISPLAY_PARENT_ARGUMENT);
//        f.getLineUsageSettings().add(DisplaySetting.DISPLAY_ARGUMENT_BRACKETED);
        f.setFooter("\nmin. Java version required : 1.6");
        f.print();
        app.exit();
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public boolean isResetOptions() {
        return resetOptions;
    }

    public boolean isMinimize() {
        return minimize;
    }

    public boolean isNosplash() {
        return nosplash;
    }
}
