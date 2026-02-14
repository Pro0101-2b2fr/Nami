package namidevelopment.kiriyaga.nami.impl.gui.component.panel;

import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.nami.impl.gui.base.BasePanel;

public class FeaturePanel extends BasePanel {

    public static final int HEIGHT = 13;

    private final Feature feature;

    public FeaturePanel(Feature feature) {
        this.feature = feature;
        this.height = HEIGHT;
    }

    @Override
    protected String getName() {
        return feature.getName();
    }

    @Override
    protected boolean isEnabled() {
        return feature.isEnabled();
    }

    @Override
    public void onLeftClick() {
        feature.toggle();
    }

    @Override
    public void onMiddleClick() {
        feature.setDrawn(!feature.isDrawn());
    }

    @Override
    public void onRightClick() {
    }

    public Feature getFeature() {
        return feature;
    }
}
