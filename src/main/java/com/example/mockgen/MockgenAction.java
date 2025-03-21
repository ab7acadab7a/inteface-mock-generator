package com.example.mockgen;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MockgenAction extends AnAction {

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

        PsiFile psiFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            Messages.showErrorDialog("No PSI file found", "Error");
            return;
        }

        List<String> interfaces = findAllInterfaces(psiFile);
        if (interfaces.isEmpty()) {
            Messages.showErrorDialog("No interfaces found in the file", "Error");
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

        for (String interfaceName : interfaces) {
            String command = generateMockgenCommand(modulePrefix, virtualFile, mocksDirectory, interfaceName);
            MockgenUtil.runMockgenCommand(command);
        }
    }

    private List<String> findAllInterfaces(PsiFile psiFile) {
        List<String> interfaces = new ArrayList<>();
        String[] lines = psiFile.getText().split("\n");
        Pattern pattern = Pattern.compile("\\s*type\\s+(\\w+)\\s+interface\\s*\\{");

        for (String line : lines) {
            Matcher matcher = pattern.matcher(line.trim());
            if (matcher.find()) {
                interfaces.add(matcher.group(1));
            }
        }
        return interfaces;
    }

    private String generateMockgenCommand(String prefix, VirtualFile file, String mocksDirectory, String interfaceName) {
        String outputFile = mocksDirectory + "/mock_" + interfaceName + ".go";
        String relativePath = file.getPath().replace(file.getParent().getPath() + "/", "");
        String finalPath = prefix + relativePath;

        String command = String.format(
                "mockgen -destination=%s -package=mocks -mock_names %s=Mock%s %s %s",
                outputFile,
                interfaceName,
                interfaceName,
                finalPath,
                interfaceName
        );

        System.out.println("Generated mockgen command: " + command);
        return command;
    }
}