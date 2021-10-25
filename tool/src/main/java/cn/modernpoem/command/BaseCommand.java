package cn.modernpoem.command;

import cn.modernpoem.bean.Poem;
import cn.modernpoem.bean.Poet;
import cn.modernpoem.util.FileHelper;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author zhy
 */
public abstract class BaseCommand implements ArgAssertable {
    private String err = "";

    /**
     * 打印帮助信息
     */
    public abstract void help();

    /**
     * 处理命令
     */
    abstract void deal0();

    public void deal() {
        ArgReader reader = ArgReader.get();
        if (!this.assertAndSave(reader)) {
            System.out.println("未知指令" + reader.getLast() + ",err:" + this.err);
            this.help();
        } else {
            this.deal0();
        }
    }

    boolean isArg(String s) {
        return s != null && s.startsWith("-");
    }

    String getArg(String s) {
        return s.substring(1);
    }

    private boolean invalidArg(String err) {
        this.err = err;
        return false;
    }

    boolean invalidArg(char c) {
        return this.invalidArg("Invalid argument: -" + c);
    }

    boolean error(String msg) {
        this.err = msg;
        return false;
    }

    void iterate(Consumer<Poet> poetConsumer, PoemHandler poemHandler) {
        this.iterate(null, poetConsumer, poemHandler, false);
    }

    void iterate(Consumer<Poet> poetConsumer,
                 PoemHandler poemHandler,
                 boolean multiThread) {
        this.iterate(null, poetConsumer, poemHandler, multiThread);
    }

    void iterate(Predicate<Poet> poetPredicate,
                 Consumer<Poet> poetConsumer,
                 Predicate<Poem> poemPredicate,
                 PoemHandler poemHandler,
                 boolean multiThread) {
        Predicate<Poet> finalPoetPredicate = poetPredicate == null ? foo -> true : poetPredicate;
        Predicate<Poem> finalPredicate = poemPredicate == null ? _ignored -> true : poemPredicate;

        FileHelper fileHelper = new FileHelper();
        List<Poet> poets = fileHelper.getAll();
        Consumer<Thread> dealMethod = multiThread ? Thread::start : Thread::run;
        List<Thread> threads = poets.stream().filter(finalPoetPredicate)
                .map(p -> new Thread(() -> {
                    List<Poem> poems = fileHelper.findByPoet(p, poemHandler != null && poemHandler.ignoreContent());
                    if (poetConsumer != null) {
                        poetConsumer.accept(p);
                    }
                    if (poemHandler != null) {
                        poems.stream().filter(finalPredicate).forEach(poemHandler);
                    }
                })).collect(Collectors.toList());
        threads.forEach(dealMethod);
        if (multiThread) {
            threads.forEach(t -> {
                try {
                    t.join();
                } catch (InterruptedException var2) {
                    var2.printStackTrace();
                }
            });
        }
    }

    void iterate(Predicate<Poet> poetPredicate,
                 Consumer<Poet> poetConsumer,
                 PoemHandler poemHandler,
                 boolean multiThread) {
        iterate(poetPredicate, poetConsumer, null, poemHandler, multiThread);
    }

    void split() {
        System.out.println("-------------------------------------");
    }
}
