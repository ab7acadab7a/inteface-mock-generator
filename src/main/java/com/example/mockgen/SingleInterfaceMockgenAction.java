package com.example.mockgen;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SingleInterfaceMockgenAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null)
            return;

        VirtualFile virtualFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile == null || !virtualFile.exists()) {
            Messages.showErrorDialog("Selected file doesn't exist or isn't valid", "Error");
            return;
        }

        Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
        if (editor == null) {
            Messages.showErrorDialog("No editor found", "Error");
            return;
        }

        PsiFile psiFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            Messages.showErrorDialog("No PSI file found", "Error");
            return;
        }

        String interfaceName = findInterfaceUnderCursor(editor, psiFile);
        if (interfaceName == null) {
            Messages.showErrorDialog("No interface found at the current cursor position", "Error");
            return;
        }

        String packageName = MockgenUtil.getPackageNameFromFile(virtualFile);
        if (packageName.isEmpty()) {
            Messages.showErrorDialog("Could not extract package name from the file", "Error");
            return;
        }

        String moduleName = MockgenUtil.getModuleNameFromGoMod(project);
        if (moduleName == null || moduleName.isEmpty()) {
            Messages.showErrorDialog("Could not extract module name from go.mod", "Error");
            return;
        }

        String modulePrefix = moduleName + "/";
        String mocksDirectory = MockgenUtil.ensureMocksDirectory(project, packageName);
        if (mocksDirectory == null)
            return;

        String command = generateMockgenCommand(modulePrefix, virtualFile, mocksDirectory, interfaceName);
        MockgenUtil.runMockgenCommand(command);
    }

    private String findInterfaceUnderCursor(Editor editor, PsiFile psiFile) {
        int lineNumber = editor.getCaretModel().getLogicalPosition().line;
        String lineText = psiFile.getText().split("\n")[lineNumber].trim();
        Pattern pattern = Pattern.compile("\\s*type\\s+(\\w+)\\s+interface\\s*\\{");
        Matcher matcher = pattern.matcher(lineText);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static String generateMockgenCommand(String moduleName, VirtualFile file, String mocksDirectory, String interfaceName) {
        String outputFile = mocksDirectory + "/mock_" + interfaceName + ".go";
        String directoryPath = file.getParent().getPath();
        String relativePath = directoryPath.equals(file.getParent().getParent().getPath()) ? "" : directoryPath.replace(file.getParent().getParent().getPath() + "/", "");
        String finalPath = moduleName + relativePath;

        String command = String.format(
                "mockgen -destination %s -package mocks -mock_names %s=Mock%s %s %s",
                escapePath(outputFile),
                interfaceName,
                interfaceName,
                escapePath(finalPath),
                interfaceName
        );

        System.out.println("Generated mockgen command: " + command);
        return command;
    }

    private static String escapePath(String path) {
        return path.replace("\\", "\\\\");
    }
}