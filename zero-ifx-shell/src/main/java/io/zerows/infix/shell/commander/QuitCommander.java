package io.zerows.infix.shell.commander;

import io.zerows.infix.shell.AbstractCommander;
import io.zerows.infix.shell.atom.CommandInput;
import io.zerows.infix.shell.cv.em.TermStatus;
import io.zerows.infix.shell.refine.Sl;

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
