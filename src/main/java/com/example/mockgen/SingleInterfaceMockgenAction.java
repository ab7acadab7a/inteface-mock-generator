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
import java.nio.file.Files;
import java.nio.file.Paths;

public class SingleInterfaceMockgenAction extends AnAction {

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

        // Find the interface under the cursor
        String interfaceName = findInterfaceUnderCursor(editor, psiFile);
        if (interfaceName == null) {
            Messages.showErrorDialog("No interface found at the current cursor position", "Error");
            return;
        }

        // Get the Go package name
        String packageName = MockgenUtil.getPackageNameFromFile(virtualFile);
        if (packageName.isEmpty()) {
            Messages.showErrorDialog("Could not extract package name from the file", "Error");
            return;
        }

        // Get the Go module name
        String moduleName = MockgenUtil.getModuleNameFromGoMod(project);
        if (moduleName == null || moduleName.isEmpty()) {
            Messages.showErrorDialog("Could not extract module name from go.mod", "Error");
            return;
        }

        // Add trailing slash to module name for path construction
        String modulePrefix = moduleName + "/";

        // Define the root mock directory
        String mocksDirectory = MockgenUtil.ensureMocksDirectory(project, packageName);
        if (mocksDirectory == null)
            return;

        // Generate and run the mockgen command for the single interface
        String command = generateMockgenCommand(modulePrefix, virtualFile, mocksDirectory, interfaceName);
        MockgenUtil.runMockgenCommand(command);
    }

    private String findInterfaceUnderCursor(Editor editor, PsiFile psiFile) {
        // Get the current line number where the cursor is located
        int lineNumber = editor.getCaretModel().getLogicalPosition().line;

        // Find the text at the current line
        String lineText = psiFile.getText().split("\n")[lineNumber].trim();

        // Regex to match an interface definition on the current line (e.g., "type
        // MyInterface interface {")
        Pattern pattern = Pattern.compile("\\s*type\\s+(\\w+)\\s+interface\\s*\\{");
        Matcher matcher = pattern.matcher(lineText);

        if (matcher.find()) {
            return matcher.group(1); // Return the interface name found
        }
        return null; // Return null if no interface is found on this line
    }

    private String generateMockgenCommand(String prefix, VirtualFile file, String mocksDirectory,
            String interfaceName) {
        // Define the destination file for the mock generation
        String outputFile = mocksDirectory + "/mock_" + interfaceName + ".go";

        // Get the relative path of the file
        String relativePath = file.getPath().replace(file.getParent().getPath() + "/", "");

        // Add the configured prefix to the relative path
        String finalPath = prefix + relativePath;

        // Build the command string with the required format
        String command = String.format(
                "mockgen -destination=%s -package=mocks -mock_names %s=Mock%s %s %s",
                outputFile,
                interfaceName, // mock name mapping
                interfaceName, // the source interface name
                finalPath, // relative path with prefix
                interfaceName // the interface name to mock
        );

        // Log the generated command
        System.out.println("Generated mockgen command: " + command);
        return command;
    }
}
