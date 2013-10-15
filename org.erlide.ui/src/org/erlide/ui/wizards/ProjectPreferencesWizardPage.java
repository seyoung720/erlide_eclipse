/*******************************************************************************
 * Copyright (c) 2004 Eric Merritt and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eric Merritt
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.ui.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.wb.swt.SWTResourceManager;
import org.erlide.backend.BackendCore;
import org.erlide.engine.model.root.ErlangProjectProperties;
import org.erlide.engine.model.root.IErlangProjectProperties;
import org.erlide.engine.model.root.PathSerializer;
import org.erlide.runtime.runtimeinfo.RuntimeVersion;
import org.erlide.ui.internal.ErlideUIPlugin;
import org.erlide.util.PreferencesUtils;
import org.erlide.util.SystemConfiguration;

/**
 * 
 * @author Eric Merritt [cyberlync at yahoo dot com]
 */
public class ProjectPreferencesWizardPage extends WizardPage {

    Text output;
    Text source;
    Text include;
    Text backendCookie;
    Combo runtimeVersion;
    Text externalModules;
    Text externalIncludes;
    private Button externalModulesBrowse;
    private Button externalIncludesBrowse;

    ErlangProjectProperties prefs;

    /**
     * Constructor inherited from parent
     * 
     * @param pageName
     * @wbp.parser.constructor
     */
    public ProjectPreferencesWizardPage(final String pageName) {
        super(pageName);
    }

    /**
     * Constructor inherited from parents parent
     * 
     * @param pageName
     * @param title
     * @param titleImage
     */
    public ProjectPreferencesWizardPage(final String pageName,
            final String title, final ImageDescriptor titleImage) {
        super(pageName, title, titleImage);
    }

    /**
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(final Composite parent) {
        prefs = new ErlangProjectProperties();
        prefs.setRuntimeVersion(BackendCore.getRuntimeInfoCatalog()
                .getDefaultRuntime().getVersion());

        // create the composite to hold the widgets
        final Composite composite = new Composite(parent, SWT.NONE);

        final String resourceString = ErlideUIPlugin
                .getResourceString("wizards.labels.buildoutput");
        composite.setLayout(new FormLayout());

        final Label lblNewLabel = new Label(composite, SWT.NONE);
        {
            final FormData fd_lblNewLabel = new FormData();
            fd_lblNewLabel.top = new FormAttachment(0, 10);
            fd_lblNewLabel.left = new FormAttachment(0, 91);
            lblNewLabel.setLayoutData(fd_lblNewLabel);
        }
        lblNewLabel.setText("Builder:");

        builder = new Combo(composite, SWT.READ_ONLY);
        builder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                enableInputWidgets(builder.getSelectionIndex() == 0);
                fillDirWidgetsFromConfig(builder.getText());
            }
        });
        {
            final FormData fd_builder = new FormData();
            fd_builder.top = new FormAttachment(0, 7);
            fd_builder.left = new FormAttachment(0, 141);
            builder.setLayoutData(fd_builder);
        }
        builder.setItems(new String[] { "internal", "rebar", "make", "emake" });
        builder.select(0);

        discoverBtn = new Button(composite, SWT.PUSH);
        {
            final FormData fd_discoverBtn = new FormData();
            fd_discoverBtn.bottom = new FormAttachment(0, 31);
            fd_discoverBtn.top = new FormAttachment(0, 5);
            fd_discoverBtn.left = new FormAttachment(0, 482);
            discoverBtn.setLayoutData(fd_discoverBtn);
        }
        discoverBtn
                .setToolTipText("Tries to guess the project's configuration \nby finding all erl and hrl files");
        discoverBtn.setText("Discover paths...");
        discoverBtn.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(final Event event) {
                discoverPaths();
            }
        });

        final Label outLabel = new Label(composite, SWT.NONE);
        outLabel.setAlignment(SWT.RIGHT);
        {
            final FormData fd_outLabel = new FormData();
            fd_outLabel.right = new FormAttachment(lblNewLabel, 0, SWT.RIGHT);
            fd_outLabel.top = new FormAttachment(0, 41);
            fd_outLabel.left = new FormAttachment(0, 5);
            outLabel.setLayoutData(fd_outLabel);
        }
        outLabel.setText("output Dir");
        outLabel.setText(resourceString + ":");
        output = new Text(composite, SWT.BORDER);
        {
            final FormData fd_output = new FormData();
            fd_output.right = new FormAttachment(0, 592);
            fd_output.top = new FormAttachment(0, 36);
            fd_output.left = new FormAttachment(0, 141);
            output.setLayoutData(fd_output);
        }
        output.setText(prefs.getOutputDir().toString());
        output.addListener(SWT.Modify, nameModifyListener);
        final String resourceString2 = ErlideUIPlugin
                .getResourceString("wizards.labels.source");
        final String resourceString3 = ErlideUIPlugin
                .getResourceString("wizards.labels.include");
        final String resourceString4 = ErlideUIPlugin
                .getResourceString("wizards.labels.testsources");

        // set the composite as the control for this page
        setControl(composite);

        final Label l1 = new Label(composite, SWT.NONE);
        l1.setAlignment(SWT.RIGHT);
        {
            final FormData fd_l1 = new FormData();
            fd_l1.right = new FormAttachment(lblNewLabel, 0, SWT.RIGHT);
            fd_l1.top = new FormAttachment(0, 71);
            fd_l1.left = new FormAttachment(0, 5);
            l1.setLayoutData(fd_l1);
        }
        l1.setText(resourceString2 + ":");
        source = new Text(composite, SWT.BORDER);
        {
            final FormData fd_source = new FormData();
            fd_source.right = new FormAttachment(0, 592);
            fd_source.top = new FormAttachment(0, 66);
            fd_source.left = new FormAttachment(0, 141);
            source.setLayoutData(fd_source);
        }
        source.setToolTipText("enter a list of folders, using / in paths and ; as list separator");
        source.setText(PathSerializer.packList(prefs.getSourceDirs()));
        source.addListener(SWT.Modify, nameModifyListener);

        final Label includesLabel = new Label(composite, SWT.NONE);
        includesLabel.setAlignment(SWT.RIGHT);
        {
            final FormData fd_includesLabel = new FormData();
            fd_includesLabel.right = new FormAttachment(lblNewLabel, 0,
                    SWT.RIGHT);
            fd_includesLabel.top = new FormAttachment(0, 101);
            fd_includesLabel.left = new FormAttachment(0, 5);
            includesLabel.setLayoutData(fd_includesLabel);
        }
        includesLabel.setText(resourceString3 + ":");
        include = new Text(composite, SWT.BORDER);
        {
            final FormData fd_include = new FormData();
            fd_include.right = new FormAttachment(0, 592);
            fd_include.top = new FormAttachment(0, 96);
            fd_include.left = new FormAttachment(0, 141);
            include.setLayoutData(fd_include);
        }
        include.setToolTipText("enter a list of folders, using / in paths and ; as list separator");
        include.setText(PathSerializer.packList(prefs.getIncludeDirs()));
        include.addListener(SWT.Modify, nameModifyListener);

        final Label lblTestSources = new Label(composite, SWT.NONE);
        lblTestSources.setAlignment(SWT.RIGHT);
        {
            final FormData fd_lblTestSources = new FormData();
            fd_lblTestSources.right = new FormAttachment(lblNewLabel, 0,
                    SWT.RIGHT);
            fd_lblTestSources.top = new FormAttachment(0, 131);
            fd_lblTestSources.left = new FormAttachment(0, 5);
            lblTestSources.setLayoutData(fd_lblTestSources);
        }
        lblTestSources.setText(resourceString4 + ":");

        test = new Text(composite, SWT.BORDER);
        {
            final FormData fd_test = new FormData();
            fd_test.right = new FormAttachment(0, 592);
            fd_test.top = new FormAttachment(0, 126);
            fd_test.left = new FormAttachment(0, 141);
            test.setLayoutData(fd_test);
        }
        test.setEditable(false);
        test.setToolTipText("enter a list of folders, using / in paths and ; as list separator");

        final Label nodeNameLabel = new Label(composite, SWT.NONE);
        nodeNameLabel.setAlignment(SWT.RIGHT);
        {
            final FormData fd_nodeNameLabel = new FormData();
            fd_nodeNameLabel.right = new FormAttachment(lblNewLabel, 44,
                    SWT.RIGHT);
            fd_nodeNameLabel.top = new FormAttachment(0, 179);
            fd_nodeNameLabel.left = new FormAttachment(0, 5);
            nodeNameLabel.setLayoutData(fd_nodeNameLabel);
        }
        nodeNameLabel.setText("Minimum Erlang version");

        runtimeVersion = new Combo(composite, SWT.READ_ONLY);
        {
            final FormData fd_runtimeVersion = new FormData();
            fd_runtimeVersion.bottom = new FormAttachment(nodeNameLabel, 0,
                    SWT.BOTTOM);
            fd_runtimeVersion.left = new FormAttachment(nodeNameLabel, 6);
            fd_runtimeVersion.right = new FormAttachment(100, -349);
            runtimeVersion.setLayoutData(fd_runtimeVersion);
        }
        final String[] runtimeVersions = getAllRuntimeVersions();
        runtimeVersion.setItems(runtimeVersions);
        runtimeVersion.setText(runtimeVersions[runtimeVersions.length - 1]);
        runtimeVersion.addListener(SWT.Modify, nameModifyListener);

        if (SystemConfiguration.getInstance().isTest()) {
            createExternalModuleEditor(composite);
            createExternalIncludeEditor(composite);
        }

    }

    protected void enableInputWidgets(final boolean b) {
        discoverBtn.setEnabled(b);
        output.setEnabled(b);
        source.setEnabled(b);
        include.setEnabled(b);
        test.setEnabled(b);
    }

    protected void fillDirWidgetsFromConfig(final String builder) {
        final WizardNewProjectCreationPage prev = (WizardNewProjectCreationPage) getPreviousPage();
        final IPath loc = prev.getLocationPath();
        final File dir = loc.toFile();

        if (!prev.getProjectName().isEmpty() && dir.exists()) {
            // TODO autodiscover project settings

        }
    }

    private String[] getAllRuntimeVersions() {
        final Collection<String> versions = BackendCore.getRuntimeInfoCatalog()
                .getAllRuntimesVersions();
        return versions.toArray(new String[versions.size()]);
    }

    protected void discoverPaths() {
        final WizardNewProjectCreationPage prev = (WizardNewProjectCreationPage) getPreviousPage();
        final IPath loc = prev.getLocationPath();
        final File dir = loc.toFile();

        if (dir.exists()) {
            final List<String> src = search("erl", dir);
            final String[] srcs = dirs(src, loc);

            final List<String> inc = search("hrl", dir);
            final String[] incs = dirs(inc, loc);

            source.setText(PreferencesUtils.packArray(srcs));
            include.setText(PreferencesUtils.packArray(incs));
        }
    }

    private String[] dirs(final List<String> list, final IPath ref) {
        final int n = ref.segmentCount();
        final List<String> res = new ArrayList<String>(10);
        for (final Iterator<String> iter = list.iterator(); iter.hasNext();) {
            final String element = iter.next();
            IPath p = new Path(element);
            p = p.removeLastSegments(1).removeFirstSegments(n).setDevice(null);
            String ps = p.toString();
            if ("".equals(ps)) {
                ps = ".";
            }
            if (res.indexOf(ps) < 0) {
                res.add(ps);
            }
        }
        return res.toArray(new String[res.size()]);
    }

    private List<String> search(final String ext, final File file) {
        return search(ext, file, new ArrayList<String>());
    }

    private List<String> search(final String ext, final File file,
            final List<String> list) {
        if (file.isFile()) {
            final IPath path = new Path(file.getPath());
            if (path.getFileExtension() != null
                    && path.getFileExtension().equals(ext)) {
                list.add(file.getPath());
            }
        } else if (file.isDirectory()) {
            final File[] fs = file.listFiles();
            for (final File f : fs) {
                search(ext, f, list);
            }
        }
        return list;
    }

    protected boolean testPageComplete() {
        if (null != output
                && (output.getText() == null || output.getText().trim()
                        .length() == 0)) {
            setErrorMessage(ErlideUIPlugin
                    .getResourceString("wizards.errors.outputrequired"));
            return false;
        }

        if (null != source
                && (source.getText() == null || source.getText().trim()
                        .length() == 0)) {
            setErrorMessage(ErlideUIPlugin
                    .getResourceString("wizards.errors.sourcerequired"));
            return false;
        }

        setErrorMessage(null);
        setMessage(null);
        return true;
    }

    private final Listener nameModifyListener = new Listener() {

        @Override
        public void handleEvent(final Event e) {
            prefs.setOutputDir(new Path(output.getText()));
            prefs.setSourceDirs(PathSerializer.unpackList(source.getText()));
            prefs.setIncludeDirs(PathSerializer.unpackList(include.getText()));
            final RuntimeVersion rv = new RuntimeVersion(
                    runtimeVersion.getText());
            prefs.setRuntimeVersion(rv);
            if (externalModules != null) {
                prefs.setExternalModulesFile(externalModules.getText());
            }
            if (externalIncludes != null) {
                prefs.setExternalIncludesFile(externalIncludes.getText());
            }

            setPageComplete(testPageComplete());
        }
    };
    private Button discoverBtn;
    private Text test;
    private Combo builder;

    public IErlangProjectProperties getPrefs() {
        return prefs;
    }

    private void createExternalModuleEditor(final Composite parent) {
        final Composite composite = parent;

        final String resourceString4 = "External modules file";
        final Label label = new Label(composite, SWT.NONE);
        {
            final FormData fd_label = new FormData();
            fd_label.top = new FormAttachment(0, 207);
            fd_label.left = new FormAttachment(0, 5);
            label.setLayoutData(fd_label);
        }
        label.setBackground(SWTResourceManager.getColor(255, 255, 183));
        label.setText(resourceString4 + ":");
        externalModules = new Text(composite, SWT.BORDER);
        {
            final FormData fd_externalModules = new FormData();
            fd_externalModules.right = new FormAttachment(0, 477);
            fd_externalModules.top = new FormAttachment(0, 202);
            fd_externalModules.left = new FormAttachment(0, 141);
            externalModules.setLayoutData(fd_externalModules);
        }
        externalModules.setToolTipText("enter a list of folders");
        externalModules.setText(prefs.getExternalModulesFile());
        externalModules.addListener(SWT.Modify, nameModifyListener);
        externalModulesBrowse = new Button(composite, SWT.NONE);
        {
            final FormData fd_externalModulesBrowse = new FormData();
            fd_externalModulesBrowse.top = new FormAttachment(0, 203);
            fd_externalModulesBrowse.left = new FormAttachment(0, 482);
            externalModulesBrowse.setLayoutData(fd_externalModulesBrowse);
        }
        externalModulesBrowse.setText("Browse...");
        externalModulesBrowse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent evt) {
                handleExternalModulesBrowseSelected();
            }

        });
    }

    private void createExternalIncludeEditor(final Composite parent) {
        final Composite composite = parent;

        final String resourceString4 = "External includes file";
        final Label label = new Label(composite, SWT.NONE);
        {
            final FormData fd_label = new FormData();
            fd_label.top = new FormAttachment(0, 237);
            fd_label.left = new FormAttachment(0, 5);
            label.setLayoutData(fd_label);
        }
        label.setBackground(SWTResourceManager.getColor(255, 255, 183));
        label.setText(resourceString4 + ":");
        externalIncludes = new Text(composite, SWT.BORDER);
        {
            final FormData fd_externalIncludes = new FormData();
            fd_externalIncludes.right = new FormAttachment(0, 477);
            fd_externalIncludes.top = new FormAttachment(0, 232);
            fd_externalIncludes.left = new FormAttachment(0, 141);
            externalIncludes.setLayoutData(fd_externalIncludes);
        }
        externalIncludes.setToolTipText("enter a list of folders");
        externalIncludes.setText(prefs.getExternalModulesFile());
        externalIncludes.addListener(SWT.Modify, nameModifyListener);
        externalIncludesBrowse = new Button(composite, SWT.NONE);
        {
            final FormData fd_externalIncludesBrowse = new FormData();
            fd_externalIncludesBrowse.top = new FormAttachment(0, 233);
            fd_externalIncludesBrowse.left = new FormAttachment(0, 482);
            externalIncludesBrowse.setLayoutData(fd_externalIncludesBrowse);
        }
        externalIncludesBrowse.setText("Browse...");
        externalIncludesBrowse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent evt) {
                handleExternalIncludesBrowseSelected();
            }

        });
    }

    protected void handleExternalModulesBrowseSelected() {
        String last = externalModules.getText();
        if (last == null) {
            last = ""; //$NON-NLS-1$
        } else {
            last = last.trim();
        }
        final FileDialog dialog = new FileDialog(getShell(), SWT.SINGLE);
        dialog.setText("Select file with external modules");
        dialog.setFileName(last);
        dialog.setFilterExtensions(new String[] { "*.erlidex" });
        final String result = dialog.open();
        if (result == null) {
            return;
        }
        externalModules.setText(result);
    }

    protected void handleExternalIncludesBrowseSelected() {
        String last = externalIncludes.getText();
        if (last == null) {
            last = ""; //$NON-NLS-1$
        } else {
            last = last.trim();
        }
        final FileDialog dialog = new FileDialog(getShell(), SWT.SINGLE);
        dialog.setText("Select file with external include files");
        dialog.setFileName(last);
        dialog.setFilterExtensions(new String[] { "*.erlidex" });
        final String result = dialog.open();
        if (result == null) {
            return;
        }
        externalIncludes.setText(result);
    }
}
