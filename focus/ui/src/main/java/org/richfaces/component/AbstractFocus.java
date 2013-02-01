package org.richfaces.component;

import org.richfaces.cdk.annotations.Attribute;
import org.richfaces.cdk.annotations.JsfComponent;
import org.richfaces.cdk.annotations.JsfRenderer;
import org.richfaces.cdk.annotations.Tag;
import org.richfaces.cdk.annotations.TagType;
import org.richfaces.log.LogFactory;
import org.richfaces.log.Logger;
import org.richfaces.renderkit.html.HtmlFocusRenderer;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@JsfComponent(tag = @Tag(name = "focus", type = TagType.Facelets),
    renderer = @JsfRenderer(family = AbstractFocus.COMPONENT_FAMILY, type = HtmlFocusRenderer.RENDERER_TYPE))
public abstract class AbstractFocus extends UIComponentBase {

    public static final String COMPONENT_FAMILY = "org.richfaces.Focus";

    public static final String COMPONENT_TYPE = "org.richfaces.Focus";

    public static final int DEFAULT_PRIORITY = Integer.MAX_VALUE;

    public static final String FOCUS_MODIFIER_FACET_NAME = "focusModifier";

    public static final String TIMING_ON_LOAD = "onload";

    private static final Logger LOG = LogFactory.getLogger(AbstractFocus.class);

    public static AbstractFocusModifier findModifier(UIComponent component) {
        if (component instanceof AbstractFocusModifier) {
            return (AbstractFocusModifier) component;
        }
        AbstractFocusModifier modifier = (AbstractFocusModifier) component.getFacet(AbstractFocus.FOCUS_MODIFIER_FACET_NAME);
        if (modifier == null) {
            for (UIComponent child : component.getChildren()) {
                modifier = findModifier(child);
                if (modifier != null) {
                    break;
                }
            }
        }
        return modifier;
    }

    @Attribute
    public abstract String getFor();

    @Attribute
    public abstract String getName();

    @Attribute
    public abstract Integer getPriority();

    @Attribute
    public abstract String getSuffix();

    @Attribute
    public abstract String getTargetClientId();

    @Attribute
    public abstract String getTiming();

    public FocusInfo findTargetComponentFocusInfo(FacesContext context) {
        String aFor = getFor();

        if (aFor != null && !"".equals(aFor)) {
            final UIComponent component = findComponent(aFor);
            if (component == null) {
                throw new FacesException("No component with id=" + aFor + " found!");
            }
            return new FocusInfo(component.getClientId(context), component, calculatePriority(component));
        } else {
            final UIComponent parent = getParent();
            if (!(parent instanceof UIInput)) {
                Set<String> allowedClientIds = new HashSet<String>();
                Iterator<String> clientIdsWithMessages = getFacesContext().getClientIdsWithMessages();
                while (clientIdsWithMessages.hasNext()) {
                    final String clientId = clientIdsWithMessages.next();
                    if (clientId != null) {
                        allowedClientIds.add(clientId);
                    }
                }
                final List<FocusInfo> inputFocusInfos = new ArrayList<FocusInfo>();
                getInputs(getParentForm(this), allowedClientIds, inputFocusInfos);
                FocusInfo inputWithLowestPriority = null;
                int lowestPriority = Integer.MAX_VALUE;
                for (FocusInfo entry : inputFocusInfos) {
                    final int priority = entry.priority;
                    if (priority < lowestPriority) {
                        inputWithLowestPriority = entry;
                        lowestPriority = priority;
                    }
                }
                return inputWithLowestPriority;
            } else {
                return new FocusInfo(parent.getClientId(context), parent, calculatePriority(parent));
            }
        }
    }

    private int calculatePriority(UIComponent component) {
        final AbstractFocusModifier modifier = findModifier(component);
        if (modifier != null && modifier.getPriority() != null) {
            return modifier.getPriority();
        }
        UIComponent parentForm = component.getParent();
        while (parentForm != null && !(parentForm instanceof UIForm)) {
            parentForm = parentForm.getParent();
        }
        if (parentForm != null) {
            return getUIInputChildrenCount(parentForm, component.getId());
        } else {
            return DEFAULT_PRIORITY;
        }
    }

    private void getInputs(UIComponent parent, final Set<String> allowedClientIds, final List<FocusInfo> focusInfos) {
        final FacesContext facesContext = getFacesContext();
        if (isCompositeComponent(parent)) {
            String parentId = parent.getId();
            parent = parent.getFacet(UIComponent.COMPOSITE_FACET_NAME);
            if (parent == null) {
                LOG.warn("Composite component " + parentId + " doesn't have facet " + UIComponent.COMPOSITE_FACET_NAME);
                return;
            }
        }
        parent.visitTree(VisitContext.createVisitContext(FacesContext.getCurrentInstance()), new VisitCallback() {
            @Override
            public VisitResult visit(VisitContext context, UIComponent target) {
                if (!target.isRendered()) {
                    return VisitResult.REJECT;
                }
                if (target instanceof UIInput && (allowedClientIds.size() == 0 || allowedClientIds.contains(target.getClientId(facesContext)))) {
                    final AbstractFocusModifier modifier = findModifier(target);
                    if (modifier == null || !modifier.isSkipped()) {
                        focusInfos.add(new FocusInfo(target.getClientId(), target, calculatePriority(target)));
                    }
                }
                return VisitResult.ACCEPT;
            }
        });
    }

    private UIForm getParentForm(UIComponent component) {
        UIComponent parent = component.getParent();
        if (parent == null) {
            return null;
        }
        if (parent instanceof UIForm) {
            return (UIForm) parent;
        } else {
            return getParentForm(parent);
        }
    }

    private int getUIInputChildrenCount(UIComponent component, String breakOnId) {
        final Holder<Integer> childrenCount = new Holder<Integer>();
        childrenCount.value = 0;
        getUIInputChildrenCount(component, breakOnId, childrenCount);
        return childrenCount.value;
    }

    private boolean getUIInputChildrenCount(UIComponent component, String breakOnId, Holder<Integer> childrenCount) {
        for (UIComponent child : component.getChildren()) {
            if (child.getId().equals(breakOnId)) {
                return true;
            }
            if (child instanceof UIInput) {
                final AbstractFocusModifier modifier = findModifier(child);
                if (modifier == null || !modifier.isSkipped()) {
                    childrenCount.value++;
                }
            } else {
                if (getUIInputChildrenCount(child, breakOnId, childrenCount)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class Holder<T> {

        public T value;
    }

    public static class FocusInfo implements Serializable {

        private String clientId;
        private UIComponent input;
        private int priority;

        private FocusInfo(String clientId, UIComponent input, int priority) {
            this.clientId = clientId;
            this.input = input;
            this.priority = priority;
        }

        public String getClientId() {
            return clientId;
        }

        public UIComponent getInput() {
            return input;
        }

        public int getPriority() {
            return priority;
        }
    }
}
