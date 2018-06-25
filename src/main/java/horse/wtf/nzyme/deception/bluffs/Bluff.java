/*
 *  This file is part of nzyme.
 *
 *  nzyme is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  nzyme is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with nzyme.  If not, see <http://www.gnu.org/licenses/>.
 */

package horse.wtf.nzyme.deception.bluffs;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import horse.wtf.nzyme.configuration.Configuration;
import horse.wtf.nzyme.util.Tools;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Map;

public abstract class Bluff {

    private static final Logger LOG = LogManager.getLogger(Bluff.class);

    protected abstract String scriptCategory();
    protected abstract String scriptName();
    protected abstract Map<String, String> parameters();

    private final Configuration configuration;

    @Nullable
    private String invokedCommand;

    private final List<String> stderr;

    public Bluff(Configuration configuration) {
        this.configuration = configuration;
        this.stderr = Lists.newArrayList();

        // Check that all parameters and the script information is safe.
        try {
            validateParameters();
        } catch (InsecureParametersException e) {
            LOG.warn("Insecure parameters passed to bluff [{}]. Refusing to execute.", this.getClass().getCanonicalName());
            throw new RuntimeException(e);
        }
    }

    public void execute() throws BluffExecutionException {
        /*
         * TODO:
         *  * secure parameter passing
         *  * describe /tmp path in README
         */

        try {
            File script = ensureScript();

            StringBuilder exec = new StringBuilder()
                    .append(configuration.getPython())
                    .append(" ")
                    .append(script.getCanonicalPath())
                    .append(" ")
                    .append("-i wlx00c0ca97120e -s javafooked -m 00:c0:ca:97:12:0e");

            this.invokedCommand = exec.toString();

            Process p = Runtime.getRuntime().exec(invokedCommand);
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            String line;
            while ((line = err.readLine()) != null) {
                stderr.add(line);
            }

            p.waitFor();
            err.close();

            if (!stderr.isEmpty()) {
                throw new BluffExecutionException("STDERR is not empty.");
            }
        } catch (InterruptedException e) {
            LOG.info("Bluff [{}] interrupted.", this.getClass().getCanonicalName(), e);
        } catch (IOException e) {
            throw new BluffExecutionException(e);
        }
    }

    public void debug() {
        LOG.info("Bluff [{}]: Invoked command {{}}.", getClass().getCanonicalName(), getInvokedCommand());

        if (stderr.isEmpty()) {
            LOG.info("Bluff [{}]: No lines written to STDERR.", getClass().getCanonicalName());
        } else {
            LOG.info("Bluff [{}]: {} lines written to STDERR:", getClass().getCanonicalName(), stderr.size());

            for (String line : stderr) {
                LOG.info("\t\tSTDERR: {}", line);
            }
        }
    }

    private void validateParameters() throws InsecureParametersException {
        if (!(Tools.isSafeParameter(configuration.getBluffDirectory()) && Tools.isSafeParameter(configuration.getBluffPrefix())
                && Tools.isSafeParameter(configuration.getPython())
                && Tools.isSafeParameter(this.getClass().getSimpleName())
                && Tools.isSafeParameter(scriptCategory()) && Tools.isSafeParameter(scriptName()))) {
            throw new InsecureParametersException();
        }
    }

    /**
     * Copies the script from the resources folder to BLUFF_DIRECTORY. We do this because the python interpreter
     * cannot reach into the .jar to execute the scripts directly.
     *
     * @return The file that holds the script
     * @throws IOException
     */
    private File ensureScript() throws IOException {
        URL url = Resources.getResource("bluffs/" + scriptCategory() + "/" + scriptName());
        String text = Resources.toString(url, Charsets.UTF_8);
        File target = new File("/" + configuration.getBluffDirectory() + "/" + configuration.getBluffPrefix() + this.getClass().getSimpleName());

        Files.asByteSink(target).write(text.getBytes());

        return target;
    }

    @Nullable
    public String getInvokedCommand() {
        return invokedCommand;
    }

    private class InsecureParametersException extends Exception {
    }

    public class BluffExecutionException extends Throwable {
        public BluffExecutionException(String s) {
            super(s);
        }

        public BluffExecutionException(Throwable t) {
            super(t);
        }
    }
}