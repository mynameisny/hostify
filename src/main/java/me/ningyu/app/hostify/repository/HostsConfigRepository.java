package me.ningyu.app.hostify.repository;


import me.ningyu.app.hostify.entity.HostsConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HostsConfigRepository extends JpaRepository<HostsConfig, Long>
{
    Optional<HostsConfig> findByConfigKey(String configKey);
    
    boolean existsByConfigKey(String configKey);
    
    boolean existsByName(String name);

    @Query("SELECT c FROM HostsConfig c LEFT JOIN FETCH c.entries WHERE c.configKey = :configKey")
    Optional<HostsConfig> findByConfigKeyWithEntries(@Param("configKey") String configKey);
    
    @Query("SELECT c FROM HostsConfig c LEFT JOIN FETCH c.entries WHERE c.id = :id")
    Optional<HostsConfig> findByIdWithEntries(@Param("id") Long id);
    
    @Query("SELECT c FROM HostsConfig c LEFT JOIN FETCH c.entries")
    List<HostsConfig> findAllWithEntries();
}
