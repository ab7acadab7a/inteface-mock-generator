package com.example.mockgen;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MockgenUtil {
    public static String getPackageNameFromFile(VirtualFile file) {
        try {
            String fileContent = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
            Pattern packagePattern = Pattern.compile("package\\s+([a-zA-Z0-9_]+)");
            Matcher matcher = packagePattern.matcher(fileContent);

            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getModuleNameFromGoMod(Project project) {
        try {
            String goModPath = project.getBasePath() + "/go.mod";
            File goModFile = new File(goModPath);
            if (goModFile.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(goModPath)), StandardCharsets.UTF_8);
                Pattern pattern = Pattern.compile("module\\s+([\\S]+)");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void runMockgenCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                Messages.showInfoMessage("Mock generated successfully!", "Success");
            } else {
                Messages.showErrorDialog("Failed to generate mock", "Error");
            }
        } catch (Exception e) {
            Messages.showErrorDialog("Error running mockgen: " + e.getMessage(), "Error");
        }
    }

    public static String ensureMocksDirectory(Project project, String packageName) {
        String mocksDirectory = project.getBasePath() + "/mocks/" + packageName;
        File dir = new File(mocksDirectory);
        if (!dir.exists() && !dir.mkdirs()) {
            Messages.showErrorDialog("Failed to create mocks directory", "Error");
            return null;
        }
        return mocksDirectory;
    }
}