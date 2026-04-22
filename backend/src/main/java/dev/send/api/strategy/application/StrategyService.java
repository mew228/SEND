package dev.send.api.strategy.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.send.api.auth.CurrentUser;
import dev.send.api.strategy.domain.StoredStrategy;
import dev.send.api.strategy.domain.StrategyKind;
import dev.send.api.strategy.domain.StrategySummary;
import dev.send.api.strategy.infra.BundledStrategyTemplateCatalog;
import dev.send.api.strategy.infra.persistence.UserStrategyStore;

@Service
@Transactional
public class StrategyService {
    private static final int MAX_STRATEGY_NAME_LENGTH = 120;

    private final BundledStrategyTemplateCatalog bundledStrategyTemplateCatalog;
    private final UserStrategyStore userStrategyStore;
    private final StrategyGraphValidator strategyGraphValidator;

    public StrategyService(
            BundledStrategyTemplateCatalog bundledStrategyTemplateCatalog,
            UserStrategyStore userStrategyStore,
            StrategyGraphValidator strategyGraphValidator) {
        this.bundledStrategyTemplateCatalog = bundledStrategyTemplateCatalog;
        this.userStrategyStore = userStrategyStore;
        this.strategyGraphValidator = strategyGraphValidator;
    }

    @Transactional(readOnly = true)
    public List<StrategySummary> findAll(Optional<CurrentUser> currentUser) {
        List<StrategySummary> templates = bundledStrategyTemplateCatalog.findAll().stream()
                .map(StoredStrategy::toSummary)
                .toList();
        if (currentUser.isEmpty()) {
            return templates;
        }

        List<StrategySummary> userStrategies = userStrategyStore.findAllByUserId(currentUser.get().id()).stream()
                .map(StoredStrategy::toSummary)
                .toList();

        return java.util.stream.Stream.concat(templates.stream(), userStrategies.stream()).toList();
    }

    @Transactional(readOnly = true)
    public Optional<StoredStrategy> findById(String id, Optional<CurrentUser> currentUser) {
        Optional<StoredStrategy> template = bundledStrategyTemplateCatalog.findById(id);
        if (template.isPresent()) {
            return template;
        }
        return currentUser.flatMap(user -> userStrategyStore.findByStrategyIdAndUserId(id, user.id()));
    }

    public StoredStrategy create(CurrentUser currentUser, StrategyUpsertCommand command) {
        strategyGraphValidator.validate(command.document());
        String normalizedName = normalizeName(command.name());
        StoredStrategy strategy = new StoredStrategy(
                "usr_" + UUID.randomUUID().toString().replace("-", ""),
                normalizedName,
                StrategyKind.USER,
                currentUser.id(),
                command.document().nodes(),
                command.document().edges(),
                Instant.now());
        return userStrategyStore.save(strategy);
    }

    public Optional<StoredStrategy> update(
            CurrentUser currentUser,
            StrategyUpsertCommand command) {
        if (command.strategyId() == null || command.strategyId().isBlank()) {
            throw new StrategyValidationException("Strategy id is required for updates.");
        }
        strategyGraphValidator.validate(command.document());
        return userStrategyStore.findByStrategyIdAndUserId(command.strategyId(), currentUser.id())
                .map(existing -> userStrategyStore.save(new StoredStrategy(
                        existing.id(),
                        normalizeName(command.name()),
                        StrategyKind.USER,
                        currentUser.id(),
                        command.document().nodes(),
                        command.document().edges(),
                        Instant.now())));
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new StrategyValidationException("Strategy name is required.");
        }
        String normalizedName = name.trim();
        if (normalizedName.length() > MAX_STRATEGY_NAME_LENGTH) {
            throw new StrategyValidationException(
                    "Strategy names are limited to " + MAX_STRATEGY_NAME_LENGTH + " characters.");
        }
        return normalizedName;
    }
}
