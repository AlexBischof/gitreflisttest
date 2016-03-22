package de.bischinger.gitdifftest;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;

import static java.util.regex.Pattern.compile;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by bischofa on 21/03/16.
 */
public class GitDiffTest {

    @ClassRule
    public static BootstrapRule bootstrapRule = new BootstrapRule();

    @Test
    public void test() throws IOException, GitAPIException {
        String searchString = "^5555$";

        Repository repository = bootstrapRule.getRepository();
        Grep grep = new Grep(repository, compile(searchString));
        assertThat(grep.grepPrintingResults()).hasSize(2);
    }
}
