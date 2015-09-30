/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.extension.repository.xwiki.internal;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.xwiki.extension.AbstractRatingExtension;
import org.xwiki.extension.DefaultExtensionAuthor;
import org.xwiki.extension.DefaultExtensionIssueManagement;
import org.xwiki.extension.DefaultExtensionScm;
import org.xwiki.extension.DefaultExtensionScmConnection;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionLicense;
import org.xwiki.extension.ExtensionLicenseManager;
import org.xwiki.extension.rating.DefaultExtensionRating;
import org.xwiki.extension.rating.RatingExtension;
import org.xwiki.extension.repository.DefaultExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionAuthor;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionDependency;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionIssueManagement;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionRating;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionRepository;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionScm;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionScmConnection;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersion;
import org.xwiki.extension.repository.xwiki.model.jaxb.License;
import org.xwiki.extension.repository.xwiki.model.jaxb.Property;

/**
 * XWiki Repository implementation of {@link org.xwiki.extension.Extension}.
 *
 * @version $Id$
 * @since 4.0M1
 */
public class XWikiExtension extends AbstractRatingExtension implements RatingExtension
{
    public XWikiExtension(XWikiExtensionRepository repository, ExtensionVersion extension,
        ExtensionLicenseManager licenseManager)
    {
        super(repository, new ExtensionId(extension.getId(), extension.getVersion()), extension.getType());

        setName(extension.getName());
        setSummary(extension.getSummary());
        setDescription(extension.getDescription());
        setWebsite(extension.getWebsite());

        setFeatures(extension.getFeatures());

        // Rating
        ExtensionRating rating = extension.getRating();
        if (rating != null) {
            setRating(new DefaultExtensionRating(rating.getTotalVotes(), rating.getAverageVote(), getRepository()));
        }

        // Authors
        for (ExtensionAuthor author : extension.getAuthors()) {
            URL url;
            try {
                url = new URL(author.getUrl());
            } catch (MalformedURLException e) {
                url = null;
            }

            addAuthor(new DefaultExtensionAuthor(author.getName(), url));
        }

        // License

        for (License license : extension.getLicenses()) {
            if (license.getName() != null) {
                ExtensionLicense extensionLicense = licenseManager.getLicense(license.getName());
                if (extensionLicense != null) {
                    addLicense(extensionLicense);
                } else {
                    List<String> content = null;
                    if (license.getContent() != null) {
                        try {
                            content = IOUtils.readLines(new StringReader(license.getContent()));
                        } catch (IOException e) {
                            // That should never happen
                        }
                    }

                    addLicense(new ExtensionLicense(license.getName(), content));
                }
            }
        }

        // Scm

        ExtensionScm scm = extension.getScm();
        if (scm != null) {
            DefaultExtensionScmConnection connection = toDefaultExtensionScmConnection(scm.getConnection());
            DefaultExtensionScmConnection developerConnection =
                toDefaultExtensionScmConnection(scm.getDeveloperConnection());

            setScm(new DefaultExtensionScm(scm.getUrl(), connection, developerConnection));
        }

        // Issue management

        ExtensionIssueManagement issueManagement = extension.getIssueManagement();
        if (issueManagement != null) {
            setIssueManagement(new DefaultExtensionIssueManagement(issueManagement.getSystem(),
                issueManagement.getUrl()));
        }

        // Category
        setCategory(extension.getCategory());

        // Properties
        for (Property property : extension.getProperties()) {
            putProperty(property.getKey(), property.getStringValue());
        }

        // Repositories
        for (ExtensionRepository restRepository : extension.getRepositories()) {
            try {
                addRepository(toDefaultExtensionRepositoryDescriptor(restRepository));
            } catch (URISyntaxException e) {
                // TODO: Log something ?
            }
        }

        // Dependencies

        for (ExtensionDependency dependency : extension.getDependencies()) {
            addDependency(new XWikiExtensionDependency(dependency));
        }

        // File

        setFile(new XWikiExtensionFile(repository, getId()));
    }

    protected static DefaultExtensionScmConnection toDefaultExtensionScmConnection(ExtensionScmConnection connection)
    {
        if (connection != null) {
            return new DefaultExtensionScmConnection(connection.getSystem(), connection.getPath());
        } else {
            return null;
        }
    }

    protected static DefaultExtensionRepositoryDescriptor toDefaultExtensionRepositoryDescriptor(
        ExtensionRepository restRepository) throws URISyntaxException
    {
        return new DefaultExtensionRepositoryDescriptor(restRepository.getId(), restRepository.getType(), new URI(
            restRepository.getUri()));
    }

    @Override
    public XWikiExtensionRepository getRepository()
    {
        return (XWikiExtensionRepository) super.getRepository();
    }
}
