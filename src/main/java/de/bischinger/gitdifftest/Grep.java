package de.bischinger.gitdifftest;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

public class Grep {

    private final Repository repository;
    private final Pattern pattern;

    public Grep(Repository repository, Pattern pattern) {
        this.repository = repository;
        this.pattern = pattern;
    }

    public List<List<String>> grepPrintingResults() throws IOException, GitAPIException {
        try (Git git = new Git(repository);
             ObjectReader objectReader = repository.newObjectReader()) {
            return stream(git.log().all().call().spliterator(), false)
                    .map(revCommit -> impl(objectReader, revCommit))
                    .filter(strings -> !strings.isEmpty())
                    .collect(toList());
        }
    }

    private List<String> impl(ObjectReader objectReader, RevCommit revCommit) {
        try {
            TreeWalk treeWalk = new TreeWalk(objectReader);

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            treeParser.reset(objectReader, revCommit.getTree());

            int treeIndex = treeWalk.addTree(treeParser);
            treeWalk.setRecursive(true);

            while (treeWalk.next()) {
                AbstractTreeIterator it = treeWalk.getTree(treeIndex, AbstractTreeIterator.class);
                ObjectId objectId = it.getEntryObjectId();
                ObjectLoader objectLoader = objectReader.open(objectId);

                if (!isBinary(objectLoader.openStream())) {
                    List<String> matchedLines = getMatchedLines(objectLoader.openStream());
                    if (!matchedLines.isEmpty()) {
                        return matchedLines.stream()
                                .map(s -> revCommit.getAuthorIdent() + " " + revCommit.getFullMessage() + ":" + s)
                                .collect(toList());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return emptyList();
    }

    private List<String> getMatchedLines(InputStream stream) throws IOException {
        try (BufferedReader buf = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
            List<String> matchedLines = new ArrayList<>();
            String line;
            while ((line = buf.readLine()) != null) {
                if (pattern.matcher(line).find()) {
                    matchedLines.add(line);
                }
            }
            return matchedLines;
        }
    }

    private static boolean isBinary(InputStream stream) throws IOException {
        try (InputStream curStream = stream) {
            return RawText.isBinary(curStream);
        }
    }
}