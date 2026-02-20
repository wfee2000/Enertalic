# Enertalic

A Hytale Library for Energy Mods

## WIP

This library is in not yet stable so expect issues.

## Importing

Following repository must be present:
```
repositories {
    maven ("https://cursemaven.com")
}
```

To include the most recent version of this library add this dependency:
```
dependencies {
    compileOnly("curse.maven:Enertalic-1435032:7652722")
}
```

To clarify usage check out [Talectrified](https://github.com/wfee2000/Talectrified)

## Components

### EnergyNode

An energy node is a block that consumes or provides energy

```json
{
  ...,
  "BlockType": {
    ...,
    "BlockEntity": {
      "Components": {
        "Enertalic:EnergyNode": {
          "CurrentEnergy": 0, // The Energy that a block is placed with
          "MaxEnergy": 100000000, // The Maximum energy
          // The Config of the block sides 
          // 0 is only input 
          // 1 is only output 
          // 2 is input and output (not supported for nodes yet) 
          // 3 is off
          "EnergySideConfig": { 
            "East": 0,
            "West": 0,
            "Up": 0,
            "Down": 0,
            "North": 0,
            "South": 0
          }
        }
      }
    }
  }
}
```

### EnergyTransfer

An energy transfer (cable) that transports energy with a certain capacity

```json
{
  ...,
  "BlockType": {
    ...,
    "BlockEntity": {
      "Components": {
        "Enertalic:EnergyTransfer": {
          "MaxTransferRate": 100, // The Maximum Transfer Rate
          // The Config of the block sides 
          // 0 is only input 
          // 1 is only output 
          // 2 is input and output
          // 3 is off
          "EnergySideConfig": {
            "East": 2,
            "West": 2,
            "Up": 2,
            "Down": 2,
            "North": 2,
            "South": 2
          }
        }
      }
    }
  }
}
```

Once a network is established (at least one input and one output that are connected) energy should start flowing from the output to the input.