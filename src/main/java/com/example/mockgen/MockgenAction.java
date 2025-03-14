package com.example.mockgen;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

public class MockgenAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null)
            return;

        // Get the currently open file
        VirtualFile virtualFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile == null || !virtualFile.exists()) {
            Messages.showErrorDialog("Selected file doesn't exist or isn't valid", "Error");
            return;
        }

        // Get the editor where the user clicked
        Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
        if (editor == null) {
            Messages.showErrorDialog("No editor found", "Error");
            return;
        }

        // Get the PsiFile (the file representation in IntelliJ PSI)
        PsiFile psiFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            Messages.showErrorDialog("No PSI file found", "Error");
            return;
        }

        // Find all interfaces in the file
        List<String> interfaceNames = findInterfacesInFile(virtualFile);
        if (interfaceNames.isEmpty()) {
            Messages.showErrorDialog("No interfaces found in the file", "Error");
            return;
        }

        // Get the Go package name
        String packageName = MockgenUtil.getPackageNameFromFile(virtualFile);
        if (packageName.isEmpty()) {
            Messages.showErrorDialog("Could not extract package name from the file", "Error");
            return;
        }

        // Define and create the mocks directory
        String mocksDirectory = MockgenUtil.ensureMocksDirectory(project, packageName);
        if (mocksDirectory == null)
            return;

        // Generate and run the mockgen command for each interface found
        for (String interfaceName : interfaceNames) {
            String command = generateMockgenCommand(virtualFile, mocksDirectory, interfaceName);
            MockgenUtil.runMockgenCommand(command);
        }
    }

    private List<String> findInterfacesInFile(VirtualFile file) {
        List<String> interfaceNames = new ArrayList<>();
        try {
            String fileContent = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
            // Regex to find all interface declarations (e.g., "type MyInterface interface
            // {")
            Pattern pattern = Pattern.compile("\\s*type\\s+(\\w+)\\s+interface\\s*\\{");
            Matcher matcher = pattern.matcher(fileContent);

            while (matcher.find()) {
                // Add each interface name found to the list
                interfaceNames.add(matcher.group(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return interfaceNames; // Return the list of interface names
    }

    private String generateMockgenCommand(VirtualFile file, String mocksDirectory, String interfaceName) {
        // Define the destination file for the mock generation
        String outputFile = mocksDirectory + "/mock_" + interfaceName + ".go";

        // Build the command string with the required format
        return String.format("mockgen -destination=%s -package=mocks -mock_names %s=Mock%s -source=%s %s",
                outputFile,
                interfaceName, // mock name mapping
                interfaceName, // the source interface name
                file.getPath(), // source file
                interfaceName); // the interface name to mock
    }
}
