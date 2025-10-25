package com.toddysoft.mspec;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Settings page for the MSpec plugin.
 */
public class MSpecSettingsConfigurable implements Configurable {

    private JPanel mainPanel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ToddySoft: MSpec Language Support";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Create header panel with logo
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Add the ToddySoft logo
        Icon logo = IconLoader.getIcon("/icons/toddy.png", MSpecSettingsConfigurable.class);
        JLabel logoLabel = new JLabel(logo);
        headerPanel.add(logoLabel);

        // Add title
        JLabel titleLabel = new JLabel("<html><h2>ToddySoft: MSpec Language Support</h2></html>");
        headerPanel.add(titleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Create info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        JLabel infoLabel = new JLabel("<html>" +
                "<p>This plugin provides language support for Apache PLC4X MSpec files.</p>" +
                "<br>" +
                "<p><b>Features:</b></p>" +
                "<ul>" +
                "<li>Syntax highlighting for MSpec files</li>" +
                "<li>Code completion for field types and data types</li>" +
                "<li>Semantic error highlighting</li>" +
                "<li>Navigate to definition (Cmd+B / Ctrl+B)</li>" +
                "<li>Cross-file type reference support</li>" +
                "<li>Code structure view</li>" +
                "</ul>" +
                "<br>" +
                "<p>For more information, visit <a href='https://toddysoft.com'>toddysoft.com</a></p>" +
                "</html>");

        infoPanel.add(infoLabel);
        mainPanel.add(infoPanel, BorderLayout.CENTER);

        return mainPanel;
    }

    @Override
    public boolean isModified() {
        // No configurable settings yet, so never modified
        return false;
    }

    @Override
    public void apply() {
        // No settings to apply yet
    }

    @Override
    public void reset() {
        // No settings to reset yet
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
    }
}
