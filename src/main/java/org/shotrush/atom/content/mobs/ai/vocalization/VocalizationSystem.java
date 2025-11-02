package org.shotrush.atom.content.mobs.ai.vocalization;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VocalizationSystem {
    
    private final Plugin plugin;
    private final HerdManager herdManager;
    private final Map<UUID, Long> callCooldowns;
    private static final long CALL_COOLDOWN_MS = 3000;
    private static final double CALL_RESPONSE_RADIUS = 30.0;
    
    public VocalizationSystem(Plugin plugin, HerdManager herdManager) {
        this.plugin = plugin;
        this.herdManager = herdManager;
        this.callCooldowns = new ConcurrentHashMap<>();
    }
    
    public void makeCall(Animals caller, CallType callType) {
        if (caller == null || !caller.isValid()) return;
        
        UUID callerId = caller.getUniqueId();
        long now = System.currentTimeMillis();
        
        Long lastCall = callCooldowns.get(callerId);
        if (lastCall != null && (now - lastCall) < CALL_COOLDOWN_MS) {
            return;
        }
        
        callCooldowns.put(callerId, now);
        
        Location callerLoc = caller.getLocation();
        if (callerLoc == null || callerLoc.getWorld() == null) return;
        
        Sound sound = getSpeciesSound(caller.getType(), callType);
        float pitch = getSoundPitch(callType);
        float volume = getSoundVolume(callType);
        
        callerLoc.getWorld().playSound(callerLoc, sound, SoundCategory.NEUTRAL, volume, pitch);
        
        triggerHerdResponse(caller, callType);
    }
    
    private void triggerHerdResponse(Animals caller, CallType callType) {
        Optional<Herd> herdOpt = herdManager.getHerd(caller.getUniqueId());
        if (herdOpt.isEmpty()) return;
        
        Herd herd = herdOpt.get();
        Location callerLoc = caller.getLocation();
        if (callerLoc == null) return;
        
        for (UUID memberId : herd.members()) {
            if (memberId.equals(caller.getUniqueId())) continue;
            
            Animals member = (Animals) Bukkit.getEntity(memberId);
            if (member == null || !member.isValid()) continue;
            
            Location memberLoc = member.getLocation();
            if (memberLoc == null || memberLoc.distance(callerLoc) > CALL_RESPONSE_RADIUS) continue;
            
            respondToCall(member, caller, callType);
        }
    }
    
    private void respondToCall(Animals responder, Animals caller, CallType callType) {
        if (responder == null || !responder.isValid()) return;
        
        Location responderLoc = responder.getLocation();
        if (responderLoc == null || responderLoc.getWorld() == null) return;
        
        responder.getScheduler().runDelayed(plugin, task -> {
            if (!responder.isValid()) return;
            
            Location loc = responder.getLocation();
            if (loc == null || loc.getWorld() == null) return;
            
            Sound responseSound = getSpeciesSound(responder.getType(), getResponseCallType(callType));
            float pitch = getSoundPitch(callType) + (float)(Math.random() * 0.2 - 0.1);
            float volume = getSoundVolume(callType) * 0.8f;
            
            loc.getWorld().playSound(loc, responseSound, SoundCategory.NEUTRAL, volume, pitch);
            
            if (callType == CallType.ALARM || callType == CallType.DISTRESS) {
                if (responder instanceof Mob mob) {
                    mob.lookAt(caller.getLocation());
                }
            }
        }, null, (long)(10 + Math.random() * 20));
    }
    
    private Sound getSpeciesSound(EntityType species, CallType callType) {
        return switch (species) {
            case COW -> switch (callType) {
                case ALARM -> Sound.ENTITY_COW_HURT;
                case CONTACT -> Sound.ENTITY_COW_AMBIENT;
                case THREAT -> Sound.ENTITY_COW_HURT;
                case DISTRESS -> Sound.ENTITY_COW_DEATH;
                case MATING -> Sound.ENTITY_COW_AMBIENT;
            };
            case SHEEP -> switch (callType) {
                case ALARM -> Sound.ENTITY_SHEEP_HURT;
                case CONTACT -> Sound.ENTITY_SHEEP_AMBIENT;
                case THREAT -> Sound.ENTITY_SHEEP_HURT;
                case DISTRESS -> Sound.ENTITY_SHEEP_DEATH;
                case MATING -> Sound.ENTITY_SHEEP_AMBIENT;
            };
            case PIG -> switch (callType) {
                case ALARM -> Sound.ENTITY_PIG_HURT;
                case CONTACT -> Sound.ENTITY_PIG_AMBIENT;
                case THREAT -> Sound.ENTITY_PIG_HURT;
                case DISTRESS -> Sound.ENTITY_PIG_DEATH;
                case MATING -> Sound.ENTITY_PIG_AMBIENT;
            };
            case WOLF -> switch (callType) {
                case ALARM -> Sound.ENTITY_WOLF_GROWL;
                case CONTACT -> Sound.ENTITY_WOLF_AMBIENT;
                case THREAT -> Sound.ENTITY_WOLF_GROWL;
                case DISTRESS -> Sound.ENTITY_WOLF_HURT;
                case MATING -> Sound.ENTITY_WOLF_WHINE;
            };
            case HORSE -> switch (callType) {
                case ALARM -> Sound.ENTITY_HORSE_ANGRY;
                case CONTACT -> Sound.ENTITY_HORSE_AMBIENT;
                case THREAT -> Sound.ENTITY_HORSE_ANGRY;
                case DISTRESS -> Sound.ENTITY_HORSE_HURT;
                case MATING -> Sound.ENTITY_HORSE_BREATHE;
            };
            case CHICKEN -> switch (callType) {
                case ALARM -> Sound.ENTITY_CHICKEN_HURT;
                case CONTACT -> Sound.ENTITY_CHICKEN_AMBIENT;
                case THREAT -> Sound.ENTITY_CHICKEN_HURT;
                case DISTRESS -> Sound.ENTITY_CHICKEN_DEATH;
                case MATING -> Sound.ENTITY_CHICKEN_EGG;
            };
            case RABBIT -> switch (callType) {
                case ALARM -> Sound.ENTITY_RABBIT_HURT;
                case CONTACT -> Sound.ENTITY_RABBIT_AMBIENT;
                case THREAT -> Sound.ENTITY_RABBIT_HURT;
                case DISTRESS -> Sound.ENTITY_RABBIT_DEATH;
                case MATING -> Sound.ENTITY_RABBIT_AMBIENT;
            };
            case LLAMA -> switch (callType) {
                case ALARM -> Sound.ENTITY_LLAMA_HURT;
                case CONTACT -> Sound.ENTITY_LLAMA_AMBIENT;
                case THREAT -> Sound.ENTITY_LLAMA_ANGRY;
                case DISTRESS -> Sound.ENTITY_LLAMA_DEATH;
                case MATING -> Sound.ENTITY_LLAMA_AMBIENT;
            };
            default -> switch (callType) {
                case ALARM -> Sound.ENTITY_GENERIC_HURT;
                case CONTACT -> Sound.ENTITY_GENERIC_SMALL_FALL;
                case THREAT -> Sound.ENTITY_GENERIC_HURT;
                case DISTRESS -> Sound.ENTITY_GENERIC_DEATH;
                case MATING -> Sound.ENTITY_GENERIC_SMALL_FALL;
            };
        };
    }
    
    private float getSoundPitch(CallType callType) {
        return switch (callType) {
            case ALARM -> 1.3f;
            case CONTACT -> 1.0f;
            case THREAT -> 0.7f;
            case DISTRESS -> 1.5f;
            case MATING -> 0.9f;
        };
    }
    
    private float getSoundVolume(CallType callType) {
        return switch (callType) {
            case ALARM -> 1.2f;
            case CONTACT -> 0.8f;
            case THREAT -> 1.5f;
            case DISTRESS -> 1.8f;
            case MATING -> 0.6f;
        };
    }
    
    private CallType getResponseCallType(CallType original) {
        return switch (original) {
            case ALARM, DISTRESS -> CallType.ALARM;
            case THREAT -> CallType.THREAT;
            case CONTACT -> CallType.CONTACT;
            case MATING -> CallType.MATING;
        };
    }
    
    public boolean shouldMakeCall(Animals animal, CallType callType) {
        if (animal == null || !animal.isValid()) return false;
        
        return switch (callType) {
            case ALARM -> animal.getLastDamage() > 0 && animal.getLastDamageCause() != null;
            case CONTACT -> Math.random() < 0.05;
            case THREAT -> animal instanceof Mob mob && mob.getTarget() != null;
            case DISTRESS -> animal.getHealth() < animal.getMaxHealth() * 0.3;
            case MATING -> animal.isAdult() && !animal.isLoveMode() && Math.random() < 0.02;
        };
    }
    
    public enum CallType {
        ALARM,
        CONTACT,
        THREAT,
        DISTRESS,
        MATING
    }
}
