package de.bischinger.gitdifftest;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * Created by bischofa on 21/03/16.
 */
public class BootstrapRule implements TestRule {

    private Repository repository;

    public BootstrapRule() {
        try {
            repository = createNewRepository();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Statement apply(Statement statement, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try (Git git = new Git(repository)) {
                    // create the file
                    File myfile = new File(repository.getDirectory().getParent(), "exportfile");
                    myfile.createNewFile();
                    myfile.deleteOnExit();

                    writeNumbersExportFile(git, myfile, 0, 100_000, null, "Adds 0 to 100_000");
                    writeNumbersExportFile(git, myfile, 0, 110_000, i -> i < 5_000 || i >= 6_000, "Adds 0 to 110_000 without 5_000s");
                    writeNumbersExportFile(git, myfile, 10_000, 100_000, null, "Adds 10_000 to 100_000");
                    writeNumbersExportFile(git, myfile, 5_000, 6_000, null, "Adds 5_000 to 6_000");
                    writeNumbersExportFile(git, myfile, 0, 1, null, "Adds 0 to 1");

                    statement.evaluate();
                } finally {
                    repository.getDirectory().deleteOnExit();
                    repository.close();
                }
            }
        };
    }

    public Repository getRepository() {
        return repository;
    }

    private void writeNumbersExportFile(Git git, File myfile, int start, int end, IntPredicate predicate, String commitMessage) throws IOException, GitAPIException {
        IntStream range = IntStream.range(start, end);
        if (predicate != null) {
            range = range.filter(predicate);
        }
        Files.write(myfile.toPath(), range.boxed().map(String::valueOf).collect(toList()));

        //add
        git.add().addFilepattern("exportfile").call();

        //commit
        git.commit().setMessage(commitMessage).call();
    }

    public static Repository createNewRepository() throws IOException {
        // prepare a new folder
        File localPath = File.createTempFile("TestGitRepository", "");
        localPath.delete();

        // create the directory
        Repository repository = FileRepositoryBuilder.create(new File(localPath, ".git"));
        repository.create();

        return repository;
    }
}
