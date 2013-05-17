/*
 * Maven Smart Modules Extension
 *
 *  http://smartmodules.googlecode.com/
 *  http://www.bitbucket.org/laurent.malvert/maven-smartmodules-extension/
 *
 * Copyright (c) 2013 Laurent Malvert
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.googlecode.maven.extensions.automodules;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;


@Component(
    role = AbstractMavenLifecycleParticipant.class,
    hint = "smartmodules"
)
public class SmartModulesExtension
        extends AbstractMavenLifecycleParticipant {

    @Requirement
    private Logger logger;

    @Requirement
    protected ProjectBuilder projectBuilder;


    @Override
    public void afterProjectsRead(final MavenSession session)
            throws MavenExecutionException {
        logger.info(" --- smartmodules-extension (after-projects-read) ---");
        final Set<MavenProject> smps = Sets.newHashSet(session.getProjects());

        session.setProjects(Lists.newArrayList(aggregate(
                session, session.getProjects(), smps
        )));
        logger.info("");
    }

    private Set<MavenProject> aggregate(final MavenSession session, final List<MavenProject> roots, final Set<MavenProject> aggregator) throws MavenExecutionException {
        final Map<MavenProject, List<File>> mm = detectModules(roots);

        logger.info("");
        logger.info("auto-detected modules...:");
        for (final Entry<MavenProject, List<File>> pe : mm.entrySet()) {
            final List<String> mns = toFilenameList(pe.getValue());

            if (!mns.isEmpty()) {
                final Set<String> smns = Sets.newHashSet(Iterables.concat(
                    pe.getKey().getModel().getModules(),
                    mns
                ));

                logger.info("  - for " + pe.getKey().getName() + ": " + smns);
                pe.getKey().getModel().setModules(Lists.newArrayList(smns));
                try {
                    final List<MavenProject> mps = toModuleProjects(session, pe.getValue());

                    aggregator.addAll(mps);
                    aggregate(session, mps, aggregator);
                } catch (final ProjectBuildingException ex) {
                    throw new MavenExecutionException(ex.getMessage(), ex);
                }
            }
        }
        return (aggregator);
    }

    private Map<MavenProject, List<File>> detectModules(final List<MavenProject> projects) {
        final Map<MavenProject, List<File>> mm = Maps.newHashMap();

        for (final MavenProject p : projects) {
            logger.info("looking for modules in: " + p.getName());
            mm.put(
                p,
                Arrays.asList(p.getBasedir().listFiles(new FileFilter() {

                    @Override
                    public boolean accept(final File child) {
                        return (new File(child, "pom.xml").exists());
                    }

                }))
            );
        }
        return (mm);
    }

    private static List<String> toFilenameList(final List<File> moduleFiles) {
        return (Lists.transform(
            moduleFiles,
            new Function<File, String>() {

                @Override
                public String apply(final File val) {
                    return (val.getName());
                }
                
            }
        ));
    }

    private List<MavenProject> toModuleProjects(final MavenSession s, final List<File> mfs)
            throws ProjectBuildingException {
        final List<MavenProject> mps = Lists.newArrayListWithCapacity(mfs.size());
        final ProjectBuildingRequest pbReq = s.getProjectBuildingRequest();
        final List<ProjectBuildingResult> pbRes = projectBuilder.build(
            Lists.transform(mfs, new Function<File, File>() {

                public File apply(final File folder) {
                    return (new File(folder, "pom.xml"));
                }
            }),
            s.getRequest().isRecursive(), pbReq
        );
        boolean problems = false;

        for (final ProjectBuildingResult r : pbRes) {
            mps.add(r.getProject());
            if (!r.getProblems().isEmpty() && logger.isWarnEnabled()) {
                logger.warn("");
                logger.warn("Some problems were encountered while building the effective model for "
                    + r.getProject().getId());
                for (final ModelProblem problem : r.getProblems()) {
                    final String location = ModelProblemUtils.formatLocation(
                        problem, r.getProjectId()
                    );

                    logger.warn(problem.getMessage() + (
                        StringUtils.isNotEmpty(location) ? " @ " + location : ""
                    ));
                }
                problems = true;
            }
        }
        if (problems) {
            logger.warn("");
            logger.warn("It is highly recommended to fix these problems"
                + " because they threaten the stability of your build.");
            logger.warn("");
            logger.warn("For this reason, future Maven versions might no"
                + " longer support building such malformed projects.");
            logger.warn("");
        }
        return (mps);
    }

}
