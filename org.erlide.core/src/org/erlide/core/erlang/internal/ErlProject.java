/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.erlide.core.erlang.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.erlide.basiccore.ErlLogger;
import org.erlide.core.ErlangPlugin;
import org.erlide.core.erlang.ErlModelException;
import org.erlide.core.erlang.ErlModelStatus;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlElement;
import org.erlide.core.erlang.IErlModelMarker;
import org.erlide.core.erlang.IErlModelStatus;
import org.erlide.core.erlang.IErlModelStatusConstants;
import org.erlide.core.erlang.IErlModule;
import org.erlide.core.erlang.IErlProject;
import org.erlide.core.erlang.util.ISuffixConstants;
import org.erlide.core.erlang.util.Util;
import org.erlide.runtime.ErlangProjectProperties;

/**
 * Handle for an Erlang Project.
 * 
 * <p>
 * A Erlang Project internally maintains a devpath that corresponds to the
 * project's classpath. The classpath may include source folders from the
 * current project; jars in the current project, other projects, and the local
 * file system; and binary folders (output location) of other projects. The
 * Erlang Model presents source elements corresponding to output .class files in
 * other projects, and thus uses the devpath rather than the classpath (which is
 * really a compilation path). The devpath mimics the classpath, except has
 * source folder entries in place of output locations in external projects.
 * 
 * <p>
 * Each ErlProject has a NameLookup facility that locates elements on by name,
 * based on the devpath.
 * 
 * @see IErlProject
 */
public class ErlProject extends Openable implements IErlProject,
		ISuffixConstants {

	/**
	 * Whether the underlying file system is case sensitive.
	 */
	protected static final boolean IS_CASE_SENSITIVE = !new File("Temp").equals(new File("temp")); //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * An empty array of strings indicating that a project doesn't have any
	 * prerequesite projects.
	 */
	protected static final String[] NO_PREREQUISITES = new String[0];

	/**
	 * The platform project this <code>IErlProject</code> is based on
	 */
	protected IProject fProject;

	/**
	 * A array with all the non-Erlang resources contained by this
	 * PackageFragment
	 */
	private IResource[] nonErlangResources;

	/**
	 * Constructor needed for <code>IProject.getNature()</code> and
	 * <code>IProject.addNature()</code>.
	 * 
	 * @see #setProject(IProject)
	 */
	public ErlProject() {
		super(null, null);
		nonErlangResources = null;
	}

	ErlProject(IProject project, ErlElement parent) {
		super(parent, project.getName());
		fProject = project;
		nonErlangResources = null;
	}

	/**
	 * Adds a builder to the build spec for the given project.
	 */
	protected void addToBuildSpec(String builderID) throws CoreException {
		final IProjectDescription description = fProject.getDescription();
		final int erlangCommandIndex = getErlangCommandIndex(description
				.getBuildSpec());

		if (erlangCommandIndex == -1) {
			// Add a Erlang command to the build spec
			final ICommand command = description.newCommand();
			command.setBuilderName(builderID);
			setErlangCommand(description, command);
		}
	}

	/**
	 * @see Openable
	 */
	@Override
	protected boolean buildStructure(IProgressMonitor pm,
			IResource underlyingResource) throws ErlModelException {
		// check whether the Erlang project can be opened
		if (!underlyingResource.isAccessible()) {
			throw newNotPresentException();
		}
		ErlLogger.debug("--- " + getProject().getName() + "? " + fChildren);
		if (fChildren != null && !fChildren.isEmpty()) {
			ErlLogger.debug("--- !");
			return true;
		}

		// final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		// final IWorkspaceRoot wRoot = workspace.getRoot();

		final ArrayList<IErlModule> modules = new ArrayList<IErlModule>(10);
		try {
			// TODO use project preferences to find which dirs are source dirs
			// final ErlangProjectProperties pprop = new
			// ErlangProjectProperties(getProject());

			final IContainer c = (IContainer) underlyingResource;
			final IResource[] elems = c.members();

			// ErlLogger.debug("--- >>> " + this.getElementName());

			// TODO should do this lazily!
			buildStructure(elems, modules);

		} catch (final CoreException e) {
			e.printStackTrace();
		}
		setChildren(modules);
		ErlLogger.debug("---YY " + fChildren.size());

		return true;
	}

	private void buildStructure(IResource[] elems, List<IErlModule> modules) {
		for (final IResource element : elems) {
			// ErlLogger.debug("---< " + elems[fi].getName());
			if (element instanceof IFolder) {
				final IFolder ff = (IFolder) element;
				try {
					buildStructure(ff.members(), modules);
				} catch (final CoreException e) {
					; // ignore
				}
			} else if (element instanceof IFile) {
				final IFile f = (IFile) element;
				final String ext = f.getFileExtension();
				if (ext != null && (ext.equals("erl") || ext.equals("hrl"))) {
					// int l = f.getFileExtension().length();
					// String mn = f.getName().substring(0, f.getName().length()
					// - l - 1);

					// ErlLogger.debug("--- l " + f.getName());
					final IErlModule m = ErlangCore.getModelManager()
							.createModuleFrom(f, this);
					modules.add(m);
				}
			}
		}

	}

	// @Override
	// protected void closing(Object info) {
	// super.closing(info);
	// }

	/**
	 * Computes the collection of modules (local ones) and set it on the given
	 * info.
	 * 
	 * @param info
	 *            ErlProjectElementInfo
	 * @throws ErlModelException
	 */
	public void computeChildren(ErlProject info) throws ErlModelException {
		// TODO fix this
		info.setNonErlangResources(null);
		info.setChildren(new ArrayList<IErlElement>());
	}

	/**
	 * Configure the project with Erlang nature.
	 */
	public void configure() throws CoreException {
		// register Erlang builder
		addToBuildSpec(ErlangPlugin.BUILDER_ID);
	}

	/*
	 * Returns whether the given resource is accessible through the children or
	 * the non-Erlang resources of this project. Returns true if the resource is
	 * not in the project. Assumes that the resource is a folder or a file.
	 */
	public boolean contains(IResource resource) {
		//
		// IClasspathEntry[] classpath;
		// IPath output;
		// try
		// {
		// classpath = getResolvedClasspath(true/* ignoreUnresolvedEntry */,
		// false/* don't generateMarkerOnError */, false/*
		// * don't
		// * returnResolutionInProgress
		// */);
		// output = getOutputLocation();
		// }
		// catch (ErlModelException e)
		// {
		// return false;
		// }
		//
		// IPath fullPath = resource.getFullPath();
		// IPath innerMostOutput = output.isPrefixOf(fullPath) ? output : null;
		// IClasspathEntry innerMostEntry = null;
		// for (int j = 0, cpLength = classpath.length; j < cpLength; j++)
		// {
		// IClasspathEntry entry = classpath[j];
		//
		// IPath entryPath = entry.getPath();
		// if ((innerMostEntry == null || innerMostEntry.getPath().isPrefixOf(
		// entryPath))
		// && entryPath.isPrefixOf(fullPath))
		// {
		// innerMostEntry = entry;
		// }
		// IPath entryOutput = classpath[j].getOutputLocation();
		// if (entryOutput != null && entryOutput.isPrefixOf(fullPath))
		// {
		// innerMostOutput = entryOutput;
		// }
		// }
		// if (innerMostEntry != null)
		// {
		// // special case prj==src and nested output location
		// if (innerMostOutput != null && innerMostOutput.segmentCount() > 1 //
		// output
		// // isn't
		// // project
		// && innerMostEntry.getPath().segmentCount() == 1)
		// { // 1 segment must be project name
		// return false;
		// }
		// if (resource instanceof IFolder)
		// {
		// // folders are always included in src/lib entries
		// return true;
		// }
		// switch (innerMostEntry.getEntryKind())
		// {
		// case IClasspathEntry.CPE_SOURCE :
		// // .class files are not visible in source folders
		// return !org.eclipse.jdt.internal.compiler.util.Util
		// .isClassFileName(fullPath.lastSegment());
		// case IClasspathEntry.CPE_LIBRARY :
		// // .Erlang files are not visible in library folders
		// return !org.eclipse.jdt.internal.compiler.util.Util
		// .isErlangFileName(fullPath.lastSegment());
		// }
		// }
		// if (innerMostOutput != null)
		// {
		// return false;
		// }
		return true;
	}

	/**
	 * TODO: Record a new marker denoting a classpath problem
	 */
	void createCodeProblemMarker(IErlModelStatus status) {
		/*
		 * final IMarker marker = null; int severity; String[] arguments = new
		 * String[0]; final boolean isCycleProblem = false,
		 * isClasspathFileFormatProblem = false; switch (status.getCode()) {
		 * 
		 * case IErlModelStatusConstants.INCOMPATIBLE_ERTS_LEVEL: final String
		 * setting = getOption( ErlangCore.CORE_INCOMPATIBLE_ERTS_LEVEL, true);
		 * if (ErlangCore.ERROR.equals(setting)) { severity =
		 * IMarker.SEVERITY_ERROR; } else if
		 * (ErlangCore.WARNING.equals(setting)) { severity =
		 * IMarker.SEVERITY_WARNING; } else { return; // setting == IGNORE }
		 * break;
		 * 
		 * default: final IPath path = status.getPath(); if (path != null) {
		 * arguments = new String[] { path.toString() }; } if
		 * (ErlangCore.ERROR.equals(getOption(
		 * ErlangCore.CORE_INCOMPLETE_CLASSPATH, true))) { severity =
		 * IMarker.SEVERITY_ERROR; } else { severity = IMarker.SEVERITY_WARNING; }
		 * break; }
		 */
	}

	/**
	 * /** Removes the Erlang nature from the project.
	 */
	public void deconfigure() throws CoreException {
		// unregister Erlang builder
		removeFromBuildSpec(ErlangPlugin.BUILDER_ID);
	}

	/**
	 * Returns a default output location. This is the project bin folder
	 */
	protected IPath defaultOutputLocation() {
		return fProject.getFullPath().append("ebin"); //$NON-NLS-1$
	}

	/**
	 * Returns true if this handle represents the same Erlang project as the
	 * given handle. Two handles represent the same project if they are
	 * identical or if they represent a project with the same underlying
	 * resource and occurrence counts.
	 * 
	 * @see ErlElement#equals(Object)
	 */
	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof ErlProject)) {
			return false;
		}

		final ErlProject other = (ErlProject) o;
		return fProject.equals(other.getProject())
				&& fOccurrenceCount == other.fOccurrenceCount;
	}

	@Override
	public boolean exists() {
		return ErlangCore.hasErlangNature(fProject);
	}

	/**
	 * @see IErlProject
	 */
	public IErlElement findElement(IPath path) throws ErlModelException {

		if (path == null || path.isAbsolute()) {
			throw new ErlModelException(new ErlModelStatus(
					IErlModelStatusConstants.INVALID_PATH, path));
		}
		/*
		 * TODO: realizate findElement(IPath path) final String extension =
		 * path.getFileExtension(); if
		 * (extension.equalsIgnoreCase(EXTENSION_ERL) ||
		 * extension.equalsIgnoreCase(EXTENSION_BEAM)) { final IPath packagePath =
		 * path.removeLastSegments(1); final String packageName =
		 * packagePath.toString().replace( IPath.SEPARATOR, '.'); String
		 * typeName = path.lastSegment(); typeName = typeName.substring(0,
		 * typeName.length() - extension.length() - 1); String qualifiedName =
		 * null; if (packageName.length() > 0) { qualifiedName = packageName +
		 * "." + typeName; //$NON-NLS-1$ } else { qualifiedName = typeName; } }
		 * else { // unsupported extension return null; }
		 */
		return null;
	}

	/**
	 * Remove all markers denoting classpath problems
	 */
	protected void flushCodepathProblemMarkers(boolean flushCycleMarkers,
			boolean flushCodepathFormatMarkers) {
		try {
			if (fProject.isAccessible()) {
				final IMarker[] markers = fProject.findMarkers(
						IErlModelMarker.BUILDPATH_PROBLEM_MARKER, false,
						IResource.DEPTH_ZERO);
				for (final IMarker marker : markers) {
					if (flushCycleMarkers && flushCodepathFormatMarkers) {
						marker.delete();
					}
				}
			}
		} catch (final CoreException e) {
			// could not flush markers: not much we can do
			if (ErlModelManager.verbose) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @see IErlElement
	 */
	public Kind getKind() {
		return Kind.PROJECT;
	}

	/**
	 * Returns the <code>char</code> that marks the start of this handles
	 * contribution to a memento.
	 */
	protected char getHandleMementoDelimiter() {

		return EM_PROJECT;
	}

	/**
	 * Find the specific Erlang command amongst the given build spec and return
	 * its index or -1 if not found.
	 */
	private int getErlangCommandIndex(ICommand[] buildSpec) {

		for (int i = 0; i < buildSpec.length; ++i) {
			if (ErlangPlugin.BUILDER_ID.equals(buildSpec[i].getBuilderName())) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns an array of non-Erlang resources contained in the receiver.
	 */
	public IResource[] getNonErlangResources() throws ErlModelException {

		return getNonErlangResources(this);
	}

	/**
	 * @see IErlProject
	 */
	public IPath getOutputLocation() throws ErlModelException {
		// Do not create marker but log problems while getting output location
		return this.getOutputLocation(false, true);
	}

	/**
	 * @param createMarkers
	 *            boolean
	 * @param logProblems
	 *            boolean
	 * @return IPath
	 * @throws ErlModelException
	 */
	public IPath getOutputLocation(boolean createMarkers, boolean logProblems)
			throws ErlModelException {

		final ErlangProjectProperties props = new ErlangProjectProperties(
				getProject());
		return new Path(props.getOutputDir());
	}

	/**
	 * @see IErlProject#getProject()
	 */
	public IProject getProject() {
		return fProject;
	}

	/**
	 * @see IErlProject#getRequiredProjectNames()
	 */
	public String[] getRequiredProjectNames() throws ErlModelException {
		return null;

		// return this.projectPrerequisites(getResolvedClasspath(true, false,
		// false));
	}

	/**
	 * @see IErlElement
	 */
	public IResource getResource() {
		return fProject;
	}

	/**
	 * @see IErlElement
	 */
	@Override
	public IResource getUnderlyingResource() throws ErlModelException {
		if (!exists()) {
			throw newNotPresentException();
		}
		return fProject;
	}

	/**
	 * @see IErlProject
	 */
	public boolean hasBuildState() {
		return ErlangCore.getModelManager().getLastBuiltState(fProject, null) != null;
	}

	@Override
	public int hashCode() {
		if (fProject == null) {
			return super.hashCode();
		}
		return fProject.hashCode();
	}

	// private IPath getPluginWorkingLocation() {
	// return fProject.getWorkingLocation(ErlangPlugin.PLUGIN_ID);
	// }

	/**
	 * Removes the given builder from the build spec for the given project.
	 */
	protected void removeFromBuildSpec(String builderID) throws CoreException {

		final IProjectDescription description = fProject.getDescription();
		final ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(builderID)) {
				final ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i,
						commands.length - i - 1);
				description.setBuildSpec(newCommands);
				fProject.setDescription(description, null);
				return;
			}
		}
	}

	/**
	 * Answers an PLUGIN_ID which is used to distinguish project/entries during
	 * package fragment root computations
	 * 
	 * @return String
	 */
	public String rootID() {
		return "[PRJ]" + fProject.getFullPath(); //$NON-NLS-1$
	}

	/**
	 * Update the Erlang command in the build spec (replace existing one if
	 * present, add one first if none).
	 */
	private void setErlangCommand(IProjectDescription description,
			ICommand newCommand) throws CoreException {
		final ICommand[] oldBuildSpec = description.getBuildSpec();
		final int oldErlangCommandIndex = getErlangCommandIndex(oldBuildSpec);
		ICommand[] newCommands;

		if (oldErlangCommandIndex == -1) {
			// Add a Erlang build spec before other builders (1FWJK7I)
			newCommands = new ICommand[oldBuildSpec.length + 1];
			System.arraycopy(oldBuildSpec, 0, newCommands, 1,
					oldBuildSpec.length);
			newCommands[0] = newCommand;
		} else {
			oldBuildSpec[oldErlangCommandIndex] = newCommand;
			newCommands = oldBuildSpec;
		}

		// Commit the spec change into the project
		description.setBuildSpec(newCommands);
		fProject.setDescription(description, null);
	}

	/**
	 * @see IErlProject
	 */
	public void setOutputLocation(IPath path, IProgressMonitor monitor)
			throws ErlModelException {
		if (path == null) {
			throw new IllegalArgumentException(Util.bind("path.nullPath")); //$NON-NLS-1$
		}
		if (path.equals(getOutputLocation())) {
			return;
		}
		// this.setRawClasspath(SetClasspathOperation.ReuseClasspath, path,
		// monitor);
	}

	/**
	 * Sets the underlying kernel project of this Erlang project, and fills in
	 * its parent and name. Called by IProject.getNature().
	 * 
	 * @see IProjectNature#setProject(IProject)
	 */
	public void setProject(IProject project) {
		fProject = project;
		fParent = ErlangCore.getModel();
		fName = project.getName();
	}

	public List<IErlModule> getModules() {
		// TODO fix this!
		return null;
		// return getChildren();
	}

	/**
	 * Returns a canonicalized path from the given external path. Note that the
	 * return path contains the same number of segments and it contains a device
	 * only if the given path contained one.
	 * 
	 * @param externalPath
	 *            IPath
	 * @see java.io.File for the definition of a canonicalized path
	 * @return IPath
	 */
	public static IPath canonicalizedPath(IPath externalPath) {

		if (externalPath == null) {
			return null;
		}

		if (IS_CASE_SENSITIVE) {
			return externalPath;
		}

		// if not external path, return original path
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		if (workspace == null) {
			return externalPath; // protection during shutdown (30487)
		}
		if (workspace.getRoot().findMember(externalPath) != null) {
			return externalPath;
		}

		IPath canonicalPath = null;
		try {
			canonicalPath = new Path(new File(externalPath.toOSString())
					.getCanonicalPath());
		} catch (final IOException e) {
			// default to original path
			return externalPath;
		}

		IPath result;
		final int canonicalLength = canonicalPath.segmentCount();
		if (canonicalLength == 0) {
			// the java.io.File canonicalization failed
			return externalPath;
		} else if (externalPath.isAbsolute()) {
			result = canonicalPath;
		} else {
			// if path is relative, remove the first segments that were added by
			// the java.io.File canonicalization
			// e.g. 'lib/classes.zip' was converted to
			// 'd:/myfolder/lib/classes.zip'
			final int externalLength = externalPath.segmentCount();
			if (canonicalLength >= externalLength) {
				result = canonicalPath.removeFirstSegments(canonicalLength
						- externalLength);
			} else {
				return externalPath;
			}
		}

		// keep device only if it was specified (this is because
		// File.getCanonicalPath() converts '/lib/classed.zip' to
		// 'd:/lib/classes/zip')
		if (externalPath.getDevice() == null) {
			result = result.setDevice(null);
		}
		return result;
	}

	/**
	 * Returns an array of non-Erlang resources contained in the receiver.
	 */
	IResource[] getNonErlangResources(ErlProject project) {

		if (nonErlangResources == null) {
			nonErlangResources = null;
		}
		return nonErlangResources;
	}

	/**
	 * Set the fNonErlangResources to res value
	 */
	void setNonErlangResources(IResource[] resources) {

		nonErlangResources = resources;
	}

	public IErlModule getModule(String name) throws ErlModelException {
		if (!hasChildren()) {
			open(null);
		}
		for (final IErlElement element : fChildren) {
			final IErlModule m = (IErlModule) element;
			if (m != null && m.getName().equals(name)) {
				return m;
			}
		}
		return null;
	}

	public boolean isVisibleInOutline() {
		return false;
	}

	@Override
	protected void closing(Object info) throws ErlModelException {
		// TODO Auto-generated method stub

	}

}