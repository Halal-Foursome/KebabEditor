package org.halalfoursome.kebabeditor.ui.navigation;

import org.halalfoursome.kebabeditor.ui.EditorView;

public interface NavigationMode {
    void attach(EditorView view);
    void detach();
    void update(double deltaSeconds);
}
