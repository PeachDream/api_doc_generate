package com.demojava01;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.event.MouseEvent;

/**
 * API文档行标记提供者
 * 在Controller类和方法的左侧显示图标，点击可生成接口文档
 *
 * @author peach
 * @since 2025/12/26 | V1.0.0
 */
public class ApiDocLineMarkerProvider implements LineMarkerProvider {

    /**
     * Gutter图标（14x14尺寸，与IDEA内置行标记图标大小一致）
     *
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private static final Icon GUTTER_ICON = IconLoader.getIcon("/META-INF/gutterIcon.svg",
            ApiDocLineMarkerProvider.class);

    @Override
    public @Nullable LineMarkerInfo<PsiElement> getLineMarkerInfo(PsiElement element) {
        if (!(element instanceof PsiIdentifier)) {
            return null;
        }

        PsiElement parent = element.getParent();
        if (parent instanceof PsiClass) {
            PsiClass psiClass = (PsiClass) parent;
            if (!isControllerClass(psiClass)) {
                return null;
            }
            return createLineMarkerInfo(element);
        }

        if (parent instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) parent;
            PsiClass containingClass = method.getContainingClass();
            if (containingClass == null || !isControllerClass(containingClass)) {
                return null;
            }
            if (!hasRequestMappingAnnotation(method)) {
                return null;
            }
            return createLineMarkerInfo(element);
        }

        return null;
    }

    /**
     * 创建行标记信息
     *
     * @param element PSI元素
     * @return 行标记信息
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private LineMarkerInfo<PsiElement> createLineMarkerInfo(PsiElement element) {
        GutterIconNavigationHandler<PsiElement> handler = (MouseEvent e, PsiElement elt) -> {
            AnAction action = ActionManager.getInstance().getAction("com.demojava01.GenerateApiDocAction");
            if (action == null) {
                return;
            }
            Component component = e.getComponent();
            ActionManager.getInstance().tryToExecute(action, e, component, "ApiDocGutter", true);
        };

        return new LineMarkerInfo<>(
                element,
                element.getTextRange(),
                GUTTER_ICON,
                psi -> "生成接口文档",
                handler,
                com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.LEFT,
                () -> "生成接口文档");
    }

    /**
     * 判断是否为Controller类
     *
     * @param psiClass PSI类
     * @return 是否为Controller类
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private boolean isControllerClass(PsiClass psiClass) {
        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList == null) {
            return false;
        }
        return hasAnnotation(modifierList, "Controller") || hasAnnotation(modifierList, "RestController");
    }

    /**
     * 判断方法是否有RequestMapping注解
     *
     * @param method PSI方法
     * @return 是否有RequestMapping注解
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private boolean hasRequestMappingAnnotation(PsiMethod method) {
        PsiModifierList modifierList = method.getModifierList();
        if (modifierList == null) {
            return false;
        }
        return hasAnnotation(modifierList, "Mapping");
    }

    /**
     * 判断是否有指定后缀的注解
     *
     * @param modifierList 修饰符列表
     * @param suffix       注解名称后缀
     * @return 是否有该注解
     * @author peach
     * @since 2025/12/26 | V1.0.0
     */
    private boolean hasAnnotation(PsiModifierList modifierList, String suffix) {
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String name = annotation.getQualifiedName();
            if (name != null && name.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
