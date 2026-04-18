# Go Plugin for IntelliJ - Development Makefile
#
# Common targets for building, testing, and running the plugin.
# Requires: Java 21+, Gradle 9+ (included via wrapper)

GRADLE      := ./gradlew
IDE_APP     := /Applications/IntelliJ IDEA 2026.2 CE EAP.app
SANDBOX     := $(CURDIR)/.sandbox-ce

.PHONY: help build compile clean run run-clean install uninstall test test-perf verify lint deps info

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

build: ## Build the plugin distribution ZIP
	$(GRADLE) buildPlugin

compile: ## Compile sources only (fast check)
	$(GRADLE) compileJava

clean: ## Clean all build outputs and sandbox
	$(GRADLE) clean
	rm -rf "$(SANDBOX)"

# ---- Running / Testing in a sandboxed CE -----------------------------------

# Prepare a clean sandbox directory with just our plugin, TextMate disabled,
# and launch the local CE installation against it.
run: build ## Launch local CE in a clean sandbox with the Go plugin
	@echo "==> Preparing sandbox at $(SANDBOX) ..."
	@mkdir -p "$(SANDBOX)/config" "$(SANDBOX)/system" "$(SANDBOX)/plugins"
	@rm -rf "$(SANDBOX)/plugins/intellij-go"
	@PLUGIN_ZIP=$$(find build/distributions -name 'intellij-go-*.zip' | head -1); \
	  unzip -qo "$$PLUGIN_ZIP" -d "$(SANDBOX)/plugins"
	@echo 'org.jetbrains.plugins.textmate' > "$(SANDBOX)/config/disabled_plugins.txt"
	@echo "==> Launching CE sandbox (TextMate Go disabled) ..."
	open -na "$(IDE_APP)" --args \
		"-Didea.config.path=$(SANDBOX)/config" \
		"-Didea.system.path=$(SANDBOX)/system" \
		"-Didea.plugins.path=$(SANDBOX)/plugins" \
		"-Didea.log.path=$(SANDBOX)/log"

run-clean: clean run ## Full clean build then launch CE sandbox

run-gradle: ## Launch via Gradle runIde (uses downloaded IDE, may be Ultimate)
	$(GRADLE) runIde

# ---- Install into real IDE --------------------------------------------------

install: build ## Install plugin into local IntelliJ IDEA CE
	@PLUGIN_ZIP=$$(find build/distributions -name 'intellij-go-*.zip' | head -1); \
	if [ -z "$$PLUGIN_ZIP" ]; then echo "ERROR: No plugin ZIP found."; exit 1; fi; \
	PLUGINS_DIR="$(IDE_APP)/Contents/plugins"; \
	echo "Installing $$PLUGIN_ZIP into $$PLUGINS_DIR ..."; \
	rm -rf "$$PLUGINS_DIR/intellij-go"; \
	unzip -qo "$$PLUGIN_ZIP" -d "$$PLUGINS_DIR"; \
	echo "Done. Restart IntelliJ IDEA to activate."

uninstall: ## Remove plugin from local IntelliJ IDEA CE
	rm -rf "$(IDE_APP)/Contents/plugins/intellij-go"
	@echo "Plugin removed. Restart IntelliJ IDEA."

# ---- Tests ------------------------------------------------------------------

test: ## Run all tests (excluding performance tests)
	$(GRADLE) test

test-perf: ## Run performance tests (downloads test data on first run)
	$(GRADLE) performanceTest

verify: build ## Run IntelliJ Plugin Verifier against the built plugin
	$(GRADLE) verifyPlugin

# ---- Utilities --------------------------------------------------------------

lint: ## Compile and check for warnings
	$(GRADLE) compileJava --warning-mode all

deps: ## Show project dependency tree
	$(GRADLE) dependencies --configuration compileClasspath

log: ## Tail the sandbox IDE log
	@tail -f "$(SANDBOX)/log/idea.log" 2>/dev/null || echo "No log yet. Run 'make run' first."

info: ## Show build environment info
	@echo "Gradle wrapper:"
	@$(GRADLE) --version | head -5
	@echo ""
	@echo "Java:"
	@java -version 2>&1 | head -3
	@echo ""
	@echo "Local IDE: $(IDE_APP)"
	@cat "$(IDE_APP)/Contents/Resources/build.txt" 2>/dev/null || echo "  (not found)"
	@echo ""
	@echo "Plugin ZIP:"
	@ls -lh build/distributions/intellij-go-*.zip 2>/dev/null || echo "  (not built yet - run 'make build')"
	@echo ""
	@echo "Sandbox: $(SANDBOX)"
	@if [ -d "$(SANDBOX)" ]; then echo "  (exists)"; else echo "  (not created yet - run 'make run')"; fi
