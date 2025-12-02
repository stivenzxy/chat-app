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
    
    int count();
    
    int countActive();
    
    List<Peer> findByDiscoveredFrom(String sourcePeerId);
}
