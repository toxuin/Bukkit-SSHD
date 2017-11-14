package com.ryanmichela.sshd;

import jline.console.ConsoleReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.StreamHandler;

public class ConsoleShellFactory implements Factory<Command> {
    public Command get() {
        return this.create();
    }

    public Command create() {
        return new ConsoleShell();
    }

    public static class ConsoleShell implements Command, Runnable {

        private InputStream in;
        private OutputStream out;
        private OutputStream err;
        private ExitCallback callback;
        private Environment environment;
        private Thread thread;

        StreamHandlerAppender streamHandlerAppender;
        ConsoleReader consoleReader;

        public InputStream getIn() {
            return in;
        }

        public OutputStream getOut() {
            return out;
        }

        public OutputStream getErr() {
            return err;
        }

        public Environment getEnvironment() {
            return environment;
        }

        public void setInputStream(InputStream in) {
            this.in = in;
        }

        public void setOutputStream(OutputStream out) {
            this.out = out;
        }

        public void setErrorStream(OutputStream err) {
            this.err = err;
        }

        public void setExitCallback(ExitCallback callback) {
            this.callback = callback;
        }

        public void start(Environment env) throws IOException {

            try {
                consoleReader = new ConsoleReader(in, new FlushyOutputStream(out), new SshTerminal());
                consoleReader.setExpandEvents(true);
                consoleReader.addCompleter(new ConsoleCommandCompleter());

                StreamHandler streamHandler = new FlushyStreamHandler(out, new ConsoleLogFormatter(), consoleReader);
                streamHandlerAppender = new StreamHandlerAppender(streamHandler);

                ((Logger) LogManager.getRootLogger()).addAppender(streamHandlerAppender);

                environment = env;
                thread = new Thread(this, "SSHD ConsoleShell " + env.getEnv().get(Environment.ENV_USER));
                thread.start();
            } catch (Exception e) {
                throw new IOException("Error starting shell", e);
            }
        }

        public void destroy() {
            ((Logger) LogManager.getRootLogger()).removeAppender(streamHandlerAppender);
        }

        public void run() {
            try {
                printPreamble(consoleReader);
                while (true) {
                    String command = consoleReader.readLine("\r>", null);
                    if (command != null) {
                        if (command.equals("exit")) {
                            break;
                        }
                        SshdPlugin.instance.getLogger().info("<" + environment.getEnv().get(Environment.ENV_USER) + "> " + command);
                        Bukkit.getScheduler().runTask(SshdPlugin.instance, () -> {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        });
                    }
                }
            } catch (IOException e) {
                SshdPlugin.instance.getLogger().severe("Error processing command from SSH");
            } finally {
                callback.onExit(0);
            }
        }

        private void printPreamble(ConsoleReader consoleReader) throws IOException {
            consoleReader.println("...................../´¯¯/)       ");
            consoleReader.println("...RCON............,/¯.../        ");
            consoleReader.println("..MY ASS.........../..../         ");
            consoleReader.println(".............../´¯/'..'/´¯¯`·¸    ");
            consoleReader.println("..........\\./'/.../..../....../¨¯ ");
            consoleReader.println("..........('(....´...´... ¯~/'..')");
            consoleReader.println("...........\\..............'...../ ");
            consoleReader.println("............\\....\\.........._.·´  ");
            consoleReader.println(".............\\..............(     ");
            consoleReader.println("..............\\..............\\    ");

            consoleReader.println("Connected to: " + Bukkit.getServer().getName());
            consoleReader.println("- " + Bukkit.getServer().getMotd());
            consoleReader.println();
            consoleReader.println("Type 'exit' to exit the shell.");
            consoleReader.println("===============================================");
        }
    }
}