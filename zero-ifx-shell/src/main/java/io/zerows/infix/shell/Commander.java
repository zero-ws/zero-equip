package io.zerows.infix.shell;

import io.horizon.eon.em.Environment;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.zerows.infix.shell.atom.CommandAtom;
import io.zerows.infix.shell.atom.CommandInput;
import io.zerows.infix.shell.cv.em.TermStatus;

/**
 * @author <a href="http://www.origin-x.cn">Lang</a>
 */
public interface Commander {

    Commander bind(Environment environment);

    Commander bind(CommandAtom options);

    Commander bind(Vertx vertx);

    TermStatus execute(CommandInput args);

    Future<TermStatus> executeAsync(CommandInput args);
}
