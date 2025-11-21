package com.serverDomain.repositories;

import com.serverDomain.entities.Peer;

import java.util.List;
import java.util.Optional;


public interface PeerRegistryRepository {
    void save(Peer peer);
    void saveOrUpdate(Peer peer);
    Optional<Peer> findById(String peerId);
    List<Peer> findAll();
    List<Peer> findAllActive();
    void markActive(String peerId);
    void markInactive(String peerId);
    void delete(String peerId);
    void deleteAll();
    
    /**
     * Counts the total number of registered peers.
     * @return Total peer count
     */
    int count();
    
    /**
     * Counts the number of active peers.
     * @return Active peer count
     */
    int countActive();
    
    /**
     * Finds all peers discovered from a specific source peer.
     * Useful for tracking transitive discovery chains.
     * @param sourcePeerId The peer ID that shared these peers
     * @return List of peers discovered from the source
     */
    List<Peer> findByDiscoveredFrom(String sourcePeerId);
}
