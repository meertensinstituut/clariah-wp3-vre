# Integration
The intregration application does two things:
- initialize VRE components 
- test components with integration tests

## 1. Initialization
- Wait for components to have started.
- Initialize kafka queues
- Upload a test file
- Initialization code is in `./src` 
- See also: 
  - `./src/main/java` 
  - `./docker-initialize.sh`

## 2. Integration testing
- Test code is in `./src/test`
- See also: 
  - `./src/main/test` 
  - `./docker-test.sh`

## Manually run integration
When all components are started, run `./start-integration.sh`
See also: `../README.md`.
