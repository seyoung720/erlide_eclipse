/*******************************************************************************
 * Copyright (c) 2008 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution.
 * 
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.ui.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchSite;
import org.erlide.core.builder.ErlangBuilder;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlModule;
import org.erlide.runtime.backend.Backend;
import org.erlide.ui.editors.erl.ErlangEditor;

public class CompileAction extends Action {

	private IWorkbenchSite site;

	public CompileAction(final IWorkbenchSite site) {
		super("Compile file");
		this.site = site;
	}

	@Override
	public void run() {
		final ErlangEditor editor = (ErlangEditor) getSite().getPage()
				.getActiveEditor();
		final IErlModule module = editor.getModule();
		final Backend b = ErlangCore.getBackendManager().getIdeBackend();

		IResource resource = module.getResource();
		IProject project = resource.getProject();
		ErlangBuilder.compileFile(project, resource, b);
	}

	public IWorkbenchSite getSite() {
		return site;
	}
}