package ru.lanwen.jenkins.juseppe.cli;

import io.airlift.airline.Command;
import ru.lanwen.jenkins.juseppe.props.JuseppeEnvVars.JuseppeEnvEnum;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.of;

/**
 * @author lanwen (Merkushev Kirill)
 */
@Command(name = "env", description = "list all available env vars to setup")
public class EnvCommand implements Runnable {

    @Override
    public void run() {
        System.out.println(of(JuseppeEnvEnum.values())
                        .map(env -> "\t%s (%s) %n\t\t- %s%n\t\tresolved: %s%n".formatted(
                                env.name(), env.mapping(), env.description(), env.resolved()))
                        .collect(joining("\n"))
        );
    }
}
