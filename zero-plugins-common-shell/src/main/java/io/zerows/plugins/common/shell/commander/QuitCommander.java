package io.zerows.plugins.common.shell.commander;

import io.zerows.plugins.common.shell.AbstractCommander;
import io.zerows.plugins.common.shell.atom.CommandInput;
import io.zerows.plugins.common.shell.cv.em.TermStatus;
import io.zerows.plugins.common.shell.refine.Sl;

/**
 * @author <a href="http://www.origin-x.cn">Lang</a>
 */
public class QuitCommander extends AbstractCommander {
    @Override
    public TermStatus execute(final CommandInput args) {
        Sl.goodbye();
        return TermStatus.EXIT;
    }
}
