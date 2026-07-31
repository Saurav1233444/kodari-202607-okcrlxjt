package com.kodari.instanceddungeons.database.repository;

import com.kodari.instanceddungeons.database.transaction.TransactionManager;
import com.kodari.instanceddungeons.repositories.CooldownRepository;
import com.kodari.instanceddungeons.repositories.DungeonDefinitionRepository;
import com.kodari.instanceddungeons.repositories.InstanceRepository;
import com.kodari.instanceddungeons.repositories.PartyRepository;
import com.kodari.instanceddungeons.repositories.PlayerProgressRepository;
import com.kodari.instanceddungeons.repositories.RewardRepository;
import com.kodari.instanceddungeons.repositories.RuntimeStateRepository;
import com.kodari.instanceddungeons.repositories.StatisticsRepository;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.concurrent.Executor;

public final class RepositoryRegistry {
    private final JdbcRepositories repositories;

    public RepositoryRegistry(DataSource dataSource, TransactionManager transactions, Executor executor) {
        this.repositories = new JdbcRepositories(
                Objects.requireNonNull(dataSource, "dataSource"),
                Objects.requireNonNull(transactions, "transactions"),
                Objects.requireNonNull(executor, "executor")
        );
    }

    public DungeonDefinitionRepository dungeonDefinitions() {
        return repositories;
    }

    public InstanceRepository instances() {
        return repositories;
    }

    public PartyRepository parties() {
        return repositories;
    }

    public PlayerProgressRepository playerProgress() {
        return repositories;
    }

    public StatisticsRepository statistics() {
        return repositories;
    }

    public RewardRepository rewards() {
        return repositories;
    }

    public CooldownRepository cooldowns() {
        return repositories;
    }

    public RuntimeStateRepository runtimeState() {
        return repositories;
    }
}