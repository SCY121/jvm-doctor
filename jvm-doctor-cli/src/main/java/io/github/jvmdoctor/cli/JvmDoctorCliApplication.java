package io.github.jvmdoctor.cli;

import picocli.CommandLine;

public final class JvmDoctorCliApplication {

    private JvmDoctorCliApplication() {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new JvmDoctorCliCommand()).execute(args);
        System.exit(exitCode);
    }
}

