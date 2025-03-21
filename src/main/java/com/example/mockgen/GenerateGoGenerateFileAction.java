package com.example.mockgen;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

        File scriptsDirectory = new File(project.getBasePath(), "scripts");
        if (!scriptsDirectory.exists() && !scriptsDirectory.mkdirs()) {
            Messages.showErrorDialog("Failed to create scripts directory", "Error");
            return;
        }

        File goGenerateFile = new File(scriptsDirectory, "mockgenGoGenerate.go");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(goGenerateFile, true))) {
            if (goGenerateFile.length() == 0) {
                writer.write("package script");
                writer.newLine();
                writer.newLine();
            }

            String command = generateGoGenerateCommand(project, virtualFile, interfaceName);
            writer.write(command);
            writer.newLine();

            Messages.showInfoMessage("go:generate command added successfully!", "Success");
        } catch (IOException ex) {
            Messages.showErrorDialog("Error creating go:generate file: " + ex.getMessage(), "Error");
        }
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

    private String generateGoGenerateCommand(Project project, VirtualFile file, String interfaceName) {
        String packageName = MockgenUtil.getPackageNameFromFile(file);
        String moduleName = MockgenUtil.getModuleNameFromGoMod(project);
        if (moduleName == null || moduleName.isEmpty()) {
            Messages.showErrorDialog("Could not extract module name from go.mod", "Error");
            return "";
        }

        String modulePrefix = moduleName + "/";
        String mocksDirectory = MockgenUtil.ensureMocksDirectory(project, packageName);
        if (mocksDirectory == null) return "";

        String command = SingleInterfaceMockgenAction.generateMockgenCommand(modulePrefix, file, mocksDirectory, interfaceName);
        return "//go:generate " + command;
    }
}