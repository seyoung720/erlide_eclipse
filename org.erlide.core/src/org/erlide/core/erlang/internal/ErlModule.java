/*******************************************************************************
 * Copyright (c) 2005 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.core.erlang.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.erlide.core.erlang.ErlModelException;
import org.erlide.core.erlang.ErlScanner;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlAttribute;
import org.erlide.core.erlang.IErlComment;
import org.erlide.core.erlang.IErlElement;
import org.erlide.core.erlang.IErlExport;
import org.erlide.core.erlang.IErlFunction;
import org.erlide.core.erlang.IErlFunctionClause;
import org.erlide.core.erlang.IErlImport;
import org.erlide.core.erlang.IErlMember;
import org.erlide.core.erlang.IErlModel;
import org.erlide.core.erlang.IErlModule;
import org.erlide.core.erlang.IErlPreprocessorDef;
import org.erlide.core.erlang.IErlProject;
import org.erlide.core.erlang.IErlScanner;
import org.erlide.core.erlang.ISourceRange;
import org.erlide.core.erlang.ISourceReference;
import org.erlide.core.util.ErlangFunction;
import org.erlide.core.util.ErlangIncludeFile;
import org.erlide.core.util.ErlideUtil;
import org.erlide.runtime.ErlLogger;
import org.erlide.runtime.backend.Backend;

import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangString;

import erlang.ErlideNoparse;

public class ErlModule extends Openable implements IErlModule {

	private long timestamp;
	private final List<IErlComment> comments;
	private String initialText;
	private IErlScanner scanner;
	private final IFile fFile;
	private boolean parsed = false;

	// These are needed to ignore the initial INSERT of all text and final
	// DELETE of all text
	private boolean fIgnoreNextReconcile = false;
	private boolean scannerDisposed = false;
	private final ModuleKind moduleKind;

	protected ErlModule(final IErlElement parent, final String name,
			final String initialText, final IFile file) {
		super(parent, name);
		fFile = file;
		moduleKind = ErlideUtil.nameToModuleKind(name);
		comments = new ArrayList<IErlComment>(0);
		scanner = null;
		this.initialText = initialText;
		// setIsStructureKnown(false);
		if (ErlModelManager.verbose) {
			ErlLogger.debug("...creating " + parent.getName() + "/" + getName()
					+ " " + moduleKind);
		}
	}

	@Override
	synchronized protected boolean buildStructure(final IProgressMonitor pm,
			IResource underlyingResource) throws ErlModelException {
		logBuildStructure(underlyingResource);
		// if (ErlModelManager.verbose) {
		// ErlLogger.debug("* build structure " + fName);
		// }

		final ErlParser parser = new ErlParser();
		if (initialText != null) { // TODO? && !isStructureKnown) {
			final String path = getFilePath();
			final String erlidePath = getErlidePath();
			isStructureKnown = parser.parse(this, initialText, !parsed, path,
					erlidePath);
		}
		parsed = isStructureKnown;
		final IErlModel model = getModel();
		if (model != null) {
			model.notifyChange(this);
		}

		// update timestamp (might be IResource.NULL_STAMP if original does not
		// exist)
		if (underlyingResource == null) {
			underlyingResource = getResource();
		}
		if (underlyingResource != null && underlyingResource instanceof IFile) {
			timestamp = ((IFile) underlyingResource).getLocalTimeStamp();
		} else {
			timestamp = IResource.NULL_STAMP;
		}

		return isStructureKnown();
	}

	/**
	 * @param underlyingResource
	 * @param path
	 * @return
	 */
	protected String getErlidePath() {
		return fFile.getFullPath().toString();
	}

	/**
	 * @param underlyingResource
	 * @return
	 */
	protected String getFilePath() {
		return fFile.getLocation().toString();
	}

	public IErlElement getElementAt(final int position)
			throws ErlModelException {
		for (final IErlElement child : fChildren) {
			if (child instanceof IErlFunction) {
				final IErlFunction f = (IErlFunction) child;
				final List<IErlFunctionClause> clauses = f.getClauses();
				if (clauses.size() <= 1
						&& f.getSourceRange().hasPosition(position)) {
					return f;
				}
				for (final IErlFunctionClause c : clauses) {
					if (c.getSourceRange().hasPosition(position)) {
						return c;
					}
				}
			} else {
				final ISourceReference ch = (ISourceReference) child;
				final ISourceRange r = ch.getSourceRange();
				if (r != null && r.hasPosition(position)) {
					return child;
				}
			}
		}
		return null;
	}

	public boolean hasResourceChanged() {
		// TODO Auto-generated method stub
		return false;
	}

	public ModuleKind getModuleKind() {
		return moduleKind;
	}

	public IResource getResource() {
		// final IResource parentRes = this.getParent().getResource();
		// if (parentRes == null || !(parentRes instanceof IContainer)) {
		// return null;
		// }
		// return ((IContainer) parentRes).getFile(new Path(getElementName()));
		return fFile;
	}

	// public String getSource() throws ErlModelException {
	// return ""; // return getBuffer().getContents();
	// }

	public ISourceRange getSourceRange() throws ErlModelException {
		return new SourceRange(0, 0);
	}

	public void copy(final IErlElement container, final IErlElement sibling,
			final String rename, final boolean replace,
			final IProgressMonitor monitor) throws ErlModelException {
		// TODO Auto-generated method stub

	}

	public void delete(final boolean force, final IProgressMonitor monitor)
			throws ErlModelException {
		// TODO Auto-generated method stub

	}

	public void move(final IErlElement container, final IErlElement sibling,
			final String rename, final boolean replace,
			final IProgressMonitor monitor) throws ErlModelException {
		// TODO Auto-generated method stub

	}

	public void rename(final String aname, final boolean replace,
			final IProgressMonitor monitor) throws ErlModelException {
		// TODO Auto-generated method stub

	}

	@Override
	protected boolean hasBuffer() {
		return true;
	}

	public void addMember(final IErlMember elem) {
		addChild(elem);
	}

	public void addComment(final IErlComment c) {
		comments.add(c);
	}

	public void reset() {
		fChildren.clear();
		comments.clear();
		isStructureKnown = false;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public List<IErlComment> getComments() {
		return comments;
	}

	public IErlImport findImport(final ErlangFunction function) {
		for (final IErlElement m : fChildren) {
			if (m instanceof IErlImport) {
				final IErlImport ei = (IErlImport) m;
				if (ei.hasFunction(function)) {
					return ei;
				}
			}
		}
		return null;
	}

	public IErlExport findExport(final ErlangFunction function) {
		for (final IErlElement m : fChildren) {
			if (m instanceof IErlExport) {
				final IErlExport ei = (IErlExport) m;
				if (ei.hasFunction(function)) {
					return ei;
				}
			}
		}
		return null;
	}

	public IErlPreprocessorDef findPreprocessorDef(final String definedName,
			final Kind type) {
		for (final IErlElement m : fChildren) {
			if (m instanceof IErlPreprocessorDef) {
				final IErlPreprocessorDef pd = (IErlPreprocessorDef) m;
				if (pd.getKind().equals(type)
						&& pd.getDefinedName().equals(definedName)) {
					return pd;
				}
			}
		}
		return null;
	}

	public List<IErlPreprocessorDef> getPreprocessorDefs(final Kind type) {
		final List<IErlPreprocessorDef> res = new ArrayList<IErlPreprocessorDef>();
		for (final IErlElement m : fChildren) {
			if (m instanceof IErlPreprocessorDef) {
				final IErlPreprocessorDef pd = (IErlPreprocessorDef) m;
				if (pd.getKind().equals(type) || type.equals(Kind.ERROR)) {
					res.add(pd);
				}
			}
		}
		return res;
	}

	public List<ErlangIncludeFile> getIncludedFiles() throws ErlModelException {
		if (!isStructureKnown) {
			open(null);
		}
		final List<ErlangIncludeFile> r = new ArrayList<ErlangIncludeFile>(0);
		for (final IErlElement m : fChildren) {
			if (m instanceof IErlAttribute) {
				final IErlAttribute a = (IErlAttribute) m;
				final OtpErlangObject v = a.getValue();
				if (v instanceof OtpErlangString) {
					final String s = ((OtpErlangString) v).stringValue();
					if ("include".equals(a.getName())) {
						r.add(new ErlangIncludeFile(false, s));
					} else if ("include_lib".equals(a.getName())) {
						r.add(new ErlangIncludeFile(true, s));
					}
				}
			}
		}
		return r;
	}

	public List<IErlImport> getImports() {
		final List<IErlImport> r = new ArrayList<IErlImport>();
		for (final IErlElement m : fChildren) {
			if (m instanceof IErlImport) {
				r.add((IErlImport) m);
			}
		}
		return r;
	}

	public IErlScanner getScanner() {
		if (scanner == null) {
			scanner = getNewScanner();
		}
		return scanner;
	}

	private IErlScanner getNewScanner() {
		final String path = getFilePath();
		final String erlidePath = getErlidePath();
		return new ErlScanner(this, initialText, path, erlidePath);
	}

	public void fixExportedFunctions() {
		try {
			final List<? extends IErlElement> functions = getChildrenOfType(IErlElement.Kind.FUNCTION);
			for (final IErlElement erlElement : functions) {
				final ErlFunction f = (ErlFunction) erlElement;
				final boolean exported = findExport(f.getFunction()) != null;
				f.setExported(exported);
			}
		} catch (final ErlModelException e) {
		}
	}

	public void reconcileText(final int offset, final int removeLength,
			final String newText, final IProgressMonitor mon) {
		if (scannerDisposed) {
			return;
		}
		// ErlLogger.debug("reconcileText " + offset + ":" + removeLength + ":"
		// + newText.length() + " ign " + fIgnoreNextReconcile);
		if (!fIgnoreNextReconcile) {
			getScanner();
			scanner.replaceText(offset, removeLength, newText);
			if (mon != null) {
				mon.worked(1);
			}
			setIsStructureKnown(false);
		}
		fIgnoreNextReconcile = false;
		try {
			open(mon);
		} catch (final ErlModelException e) {
			ErlLogger.warn(e);
		}
	}

	@Override
	protected void closing(final Object info) throws ErlModelException {
		// TODO Auto-generated method stub

	}

	public void finalReconcile() {
		fIgnoreNextReconcile = true;
	}

	public void initialReconcile() {
		fIgnoreNextReconcile = true;
	}

	private String getModuleName() {
		return ErlideUtil.withoutExtension(getName());
	}

	public void disposeScanner() {
		// TODO use reference counting to know if there are any editors on the
		// module
		if (scanner == null) {
			return;
		}
		scanner.dispose();
		scanner = null;
		scannerDisposed = true;
	}

	public void disposeParser() {
		final Backend b = ErlangCore.getBackendManager().getIdeBackend();
		ErlideNoparse.destroy(b, getModuleName());
	}

	public void reenableScanner() {
		scannerDisposed = false;
	}

	public IErlProject getProject() {
		for (IErlElement p = this; p != null; p = p.getParent()) {
			if (p instanceof IErlProject) {
				return (IErlProject) p;
			}
		}
		return null;
	}

	public Kind getKind() {
		return Kind.MODULE;
	}

	public void dispose() {
	}

}
