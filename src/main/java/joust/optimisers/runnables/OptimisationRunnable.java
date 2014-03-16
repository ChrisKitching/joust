package joust.optimisers.runnables;

import joust.optimisers.translators.BaseTranslator;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.LogUtils;

import java.lang.reflect.InvocationTargetException;

import static joust.Optimiser.inputTrees;

/**
 * Base class for optimisation runnables. Each one represents a single optimisation task.
 */
public abstract class OptimisationRunnable implements Runnable {
    @Override
    public abstract void run();

    abstract static class TreeProcessing extends OptimisationRunnable {
        @Override
        public void run() {
            for (AJCTree tree : inputTrees.rootNodes) {
                processRootNode(tree);
            }
        }

        protected abstract void processRootNode(AJCTree node);
    }

    /**
     * Base class for runnables that involve the use of a single translator instance (To be created in the constructor and
     * reused thereafter).
     */
    abstract static class SingleTranslatorInstance extends TreeProcessing {
        protected BaseTranslator translatorInstance;

        public SingleTranslatorInstance(Class<? extends BaseTranslator> clazz) {
            try {
                translatorInstance = clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                LogUtils.raiseCompilerError("Exception thrown from OneShot while instantiating translator: " + e);
            }
        }
    }

    abstract static class OneShot extends SingleTranslatorInstance {
        public OneShot(Class<? extends BaseTranslator> clazz) {
            super(clazz);
        }

        @Override
        public void processRootNode(AJCTree node) {
            // Apply the visitor once to each tree.
            translatorInstance.visit(node);
        }
    }

    abstract static class BluntForce extends SingleTranslatorInstance {
        public BluntForce(Class<? extends BaseTranslator> clazz) {
            super(clazz);
        }

        @Override
        protected void processRootNode(AJCTree node) {
            do {
                translatorInstance.visit(node);
            } while(translatorInstance.makingChanges());
        }
    }
}
