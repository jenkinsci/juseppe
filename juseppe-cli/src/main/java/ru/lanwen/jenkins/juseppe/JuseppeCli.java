package ru.lanwen.jenkins.juseppe;

import io.airlift.airline.Cli;
import io.airlift.airline.Cli.CliBuilder;
import io.airlift.airline.Help;
import ru.lanwen.jenkins.juseppe.cli.EnvCommand;
import ru.lanwen.jenkins.juseppe.cli.GenerateCommand;
import ru.lanwen.jenkins.juseppe.cli.JuseppeCommand;
import ru.lanwen.jenkins.juseppe.cli.PrintCertCommand;
import ru.lanwen.jenkins.juseppe.cli.ServeCommand;

/**
 * @author lanwen (Merkushev Kirill)
 */
public final class JuseppeCli {
    private JuseppeCli() {
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        CliBuilder<Runnable> builder = Cli.<Runnable>builder("juseppe")
                .withDescription("Jenkins Update Site Embedded for Plugin Publishing Easily")
                .withDefaultCommand(Help.class)
                .withCommands(Help.class,
                        ServeCommand.class,
                        GenerateCommand.class,
                        EnvCommand.class,
                        PrintCertCommand.class
                );

        Runnable parse = builder.build().parse(args);
        parse.run();

        if (parse instanceof JuseppeCommand command) {
            System.exit(command.getExitCode());
        }
    }

}
