package com.kodari.instanceddungeons.database.repository;

import com.kodari.instanceddungeons.database.transaction.TransactionManager;
import com.kodari.instanceddungeons.repositories.CooldownRepository;
import com.kodari.instanceddungeons.repositories.DungeonDefinitionRepository;
import com.kodari.instanceddungeons.repositories.InstanceRepository;
import com.kodari.instanceddungeons.repositories.PlayerProgressRepository;
import com.kodari.instanceddungeons.repositories.RewardRepository;
import com.kodari.instanceddungeons.repositories.RuntimeStateRepository;
import com.kodari.instanceddungeons.repositories.StatisticsRepository;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.concurrent.Executor;

public final class RepositoryRegistry {
    private final DungeonDefinitionRepository dungeonDefinitionRepository;
    private final InstanceRepository instanceRepository;
    private final PlayerProgressRepository playerProgressRepository;
    private final StatisticsRepository statisticsRepository;
    private final RewardRepository rewardRepository;
    private final CooldownRepository cooldownRepository;
    private final RuntimeStateRepository runtimeStateRepository;

    public RepositoryRegistry(DataSource dataSource, TransactionManager transactions, Executor executor) {
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(transactions, "transactions");
        Objects.requireNonNull(executor, "executor");

        this.dungeonDefinitionRepository = new JdbcDungeonDefinitionRepository(dataSource, executor);
        this.instanceRepository = new JdbcInstanceRepository(dataSource, executor);
        this.playerProgressRepository = new JdbcPlayerProgressRepository(dataSource, executor);
        this.statisticsRepository = new JdbcStatisticsRepository(dataSource, executor);
        this.rewardRepository = new JdbcRewardRepository(dataSource, executor);
        this.cooldownRepository = new JdbcCooldownRepository(dataSource, executor);
        this.runtimeStateRepository = new JdbcRuntimeStateRepository(dataSource, executor);
    }

    public DungeonDefinitionRepository dungeonDefinitions() {
        return dungeonDefinitionRepository;
    }

    public InstanceRepository instances() {
        return instanceRepository;
    }

    public PlayerProgressRepository playerProgress() {
        return playerProgressRepository;
    }

    public StatisticsRepository statistics() {
        return statisticsRepository;
    }

    public RewardRepository rewards() {
        return rewardRepository;
    }

    public CooldownRepository cooldowns() {
        return cooldownRepository;
    }

    public RuntimeStateRepository runtimeState() {
        return runtimeStateRepository;
    }
}