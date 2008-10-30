/*
 *   $Id$
 *
 *   Copyright 2007 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ome.conditions.ApiUsageException;
import ome.conditions.InternalException;
import ome.model.annotations.TextAnnotation;
import ome.model.core.Image;
import ome.system.ServiceFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyTermEnum;
import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.store.DirectoryProvider;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

/**
 * Search to find similar terms to some given terms.
 * 
 * @author Josh Moore, josh at glencoesoftware.com
 * @since Beta4
 */
public class SimilarTerms extends SearchAction {

    private static final Log log = LogFactory.getLog(SimilarTerms.class);

    private static final long serialVersionUID = 1L;

    private final String[] terms;

    public SimilarTerms(SearchValues values, String...terms) {
        super(values);
        this.terms = terms;
    }

    public Object doWork(TransactionStatus status, Session s, ServiceFactory sf) {

        if (values.onlyTypes == null || values.onlyTypes.size() != 1) {
            throw new ApiUsageException(
                    "Searches by similar terms are currently limited to a single type.\n"
                            + "Plese use Search.onlyType()");
        }
        final Class<?> cls = values.onlyTypes.get(0);

        final FullTextSession session = Search.createFullTextSession(s);
        final SearchFactory factory = session.getSearchFactory();
        final DirectoryProvider[] directory = factory.getDirectoryProviders(cls);
        final ReaderProvider provider = factory.getReaderProvider();

        Assert.notEmpty(directory, "Must have a directory provider");
        Assert.isTrue(directory.length == 1, "Can only handle one directory");
        
        final IndexReader reader = provider.openReader(directory[0]);
        
        FuzzyTermEnum fuzzy = null;
        List<TextAnnotation> rv = new ArrayList<TextAnnotation>();
        try {
            fuzzy = new FuzzyTermEnum(reader, new Term("combined_fields", terms[0]));
            while (fuzzy.next()) {
                TextAnnotation text = new TextAnnotation();
                text.setNs(terms[0]);
                text.setTextValue(fuzzy.term().text());
                rv.add(text);
            }
            return rv;
        } catch (IOException e) {
            throw new InternalException("Error reading from index: "+e.getMessage());
        } finally {
            if (fuzzy != null) {
                fuzzy.endEnum();
            }
        }
        
    }
}
