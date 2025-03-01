package com.example.mockgen;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateGoGenerateFileAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        VirtualFile virtualFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile == null || !virtualFile.exists()) {
            Messages.showErrorDialog("Selected file doesn't exist or isn't valid", "Error");
            return;
        }

        // Define the path where the go:generate file will be created
        File scriptsDirectory = new File(project.getBasePath(), "scripts");
        if (!scriptsDirectory.exists() && !scriptsDirectory.mkdirs()) {
            Messages.showErrorDialog("Failed to create scripts directory", "Error");
            return;
        }

        File goGenerateFile = new File(scriptsDirectory, "mockgenGoGenerate.go");

        // Write the go:generate commands into the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(goGenerateFile))) {
            // Write the package declaration at the top
            writer.write("package script");
            writer.newLine();
            writer.newLine();  // Blank line after package declaration

            // Derive the go:generate command from the selected interface file
            String command = generateGoGenerateCommand(virtualFile);
            writer.write(command);
            writer.newLine();  // You can add more commands if needed

            Messages.showInfoMessage("go:generate file created successfully!", "Success");
        } catch (IOException ex) {
            Messages.showErrorDialog("Error creating go:generate file: " + ex.getMessage(), "Error");
        }
    }

    private String generateGoGenerateCommand(VirtualFile file) {
        // Get the Go package name from the selected file's contents
        String packageName = getPackageNameFromFile(file);

        // Generate the go:generate command
        String interfaceName = file.getNameWithoutExtension(); // Assuming the interface name is the file name without extension
        return "//go:generate mockgen -source=" + file.getPath() + " -destination=mocks/" + packageName + "/" + interfaceName + "_mock.go -package=mocks";
    }

    private String getPackageNameFromFile(VirtualFile file) {
        try {
            String fileContent = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
            Pattern packagePattern = Pattern.compile("package\\s+([a-zA-Z0-9_]+)");
            Matcher matcher = packagePattern.matcher(fileContent);

            if (matcher.find()) {
                return matcher.group(1); // Extract the package name
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
