package joust.optimisers.runnables;

import joust.optimisers.translators.BaseTranslator;
import joust.tree.annotatedtree.AJCForest;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.logging.LogUtils;
import lombok.AllArgsConstructor;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

/**
 * Base class for optimisation runnables. Each one represents a single optimisation task.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public abstract class OptimisationRunnable implements Runnable {
    public static final String NAME_CORE = "Toast";

    @Override
    public abstract void run();

    public abstract String getName();

    abstract static class TreeProcessing extends OptimisationRunnable {
        @Override
        public void run() {
            log.info("Applying {} to {} nodes", getClass().getSimpleName(), AJCForest.getInstance().rootNodes.size());
            for (AJCTree tree : AJCForest.getInstance().rootNodes) {
                AJCForest.getInstance().setEnvironment(tree);
                processRootNode(tree);
            }
        }

        protected void logExecution(AJCTree node) {
            if (node instanceof AJCTree.AJCClassDecl) {
                AJCTree.AJCClassDecl clazz = (AJCTree.AJCClassDecl) node;
                log.info("Running {} on {}", getClass().getSimpleName(), clazz.getSym());
            }
        }

        protected abstract void processRootNode(AJCTree node);
    }

    /**
     * Base class for runnables that involve the use of a single translator instance (To be created in the constructor and
     * reused thereafter).
     */
    @AllArgsConstructor
    abstract static class SingleTranslatorInstance extends TreeProcessing {
        protected final BaseTranslator translatorInstance;

        @Override
        public String getName() {
            return translatorInstance.getClass().getSimpleName();
        }
    }

    /**
     * A translator that applies the translator instance once to every tree.
     */
    abstract static class OneShot extends SingleTranslatorInstance {
        public OneShot(BaseTranslator translator) {
            super(translator);
        }

        @Override
        public void processRootNode(AJCTree node) {
            logExecution(node);

            // Apply the visitor once to each tree.
            translatorInstance.visitTree(node);
        }
    }

    /**
     * A translator that repeatedly applies the translator instance until it ceases to make changes.
     */
    abstract static class BluntForce extends SingleTranslatorInstance {
        public BluntForce(BaseTranslator translator) {
            super(translator);
        }

        @Override
        protected void processRootNode(AJCTree node) {
            logExecution(node);

            boolean modified = false;
            do {
                if (modified) {
                    AJCForest.getInstance().statisticsManager.touchedFile(AJCForest.currentEnvironment);
                }

                translatorInstance.visitTree(node);
                modified = true;
            } while(translatorInstance.makingChanges());
        }
    }

    /**
     * Blunt-force apply the primary, then the secondaryTranslatorInstance, then the primary again...
     */
    @AllArgsConstructor
    abstract static class OneTwo extends TreeProcessing {
        private final BaseTranslator primaryTranslatorInstance;
        private final BaseTranslator secondaryTranslatorInstance;

        @Override
        protected void processRootNode(AJCTree node) {
            logExecution(node);

            boolean modified = false;

            do {
                if (modified) {
                    AJCForest.getInstance().statisticsManager.touchedFile(AJCForest.currentEnvironment);
                }

                primaryTranslatorInstance.visitTree(node);

                if (node instanceof AJCTree.AJCClassDecl) {
                    AJCTree.AJCClassDecl clazz = (AJCTree.AJCClassDecl) node;
                    log.info("Running {} on {}", secondaryTranslatorInstance.getClass().getSimpleName(), clazz.getSym());
                }

                do {
                    if (modified) {
                        AJCForest.getInstance().statisticsManager.touchedFile(AJCForest.currentEnvironment);
                    }

                    secondaryTranslatorInstance.visitTree(node);

                    modified = true;
                } while (secondaryTranslatorInstance.makingChanges());
            } while (primaryTranslatorInstance.makingChanges());
        }

        @Override
        public String getName() {
            return primaryTranslatorInstance.getClass().getSimpleName();
        }
    }
}
