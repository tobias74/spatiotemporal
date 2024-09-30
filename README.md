# R-Tree Geospatial Indexing API

This project provides a geospatial indexing API using an R-tree structure to store and query geographical data points.
It supports geospatial queries like finding the nearest neighbors to a point, inserting and deleting geospatial data,
and handling underflows within the R-tree.

## Features

- **Geopoint Insertion**: Insert geopoints into an R-tree using latitude, longitude, and timestamps.
- **Nearest Neighbor Search**: Query nearest geopoints within a specified time range, sorted by their distance to a
  query point.
- **Geopoint Deletion**: Delete geopoints based on an external ID.
- **R-tree Structure**: Supports dynamic splitting of nodes based on node capacity and rebalances the tree during
  underflows.
- **Efficient Queries**: Use a combination of bounding boxes and spatial indexing to find data points efficiently.
- **Surface Distance Calculation**: The Haversine formula is used to calculate the distance on the surface of the Earth
  between geopoints.

## Getting Started

### Prerequisites

- **Java 17+**: The project is written in Java and requires a Java development kit.
- **Spring Boot**: Used as the framework for building the API.
- **SQLite**: Used as the backend database for storing R-tree nodes and data points.
- **Gradle**: Used for building the project.

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/tobias74/spatiotemporal
   cd spatiotemporal
   ```

2. Build the project using Gradle:

   ```bash
   ./gradlew build
   ```

3. Run the Spring Boot application:

   ```bash
   ./gradlew bootRun
   ```

### API Endpoints

#### 1. Insert Geopoint

**Endpoint**: `POST /api/geopoints/insert`

**Description**: Inserts a new geopoint into the R-tree using latitude, longitude, timestamp, and an external ID.

**Request Parameters**:

- `lat` (double): Latitude of the geopoint.
- `lon` (double): Longitude of the geopoint.
- `timestamp` (long): Timestamp associated with the geopoint.
- `externalId` (String): External identifier for the geopoint.

**Example**:

```bash
curl -X POST "http://localhost:8080/api/geopoints/insert"      -d "lat=37.7749&lon=-122.4194&timestamp=1630454400000&externalId=myGeopoint"
```

#### 2. Delete Geopoint

**Endpoint**: `DELETE /api/geopoints/delete`

**Description**: Deletes geopoints matching the provided external ID.

**Request Parameters**:

- `externalId` (String): External identifier for the geopoints to delete.

**Example**:

```bash
curl -X DELETE "http://localhost:8080/api/geopoints/delete?externalId=myGeopoint"
```

#### 3. Get Nearest Geopoints

**Endpoint**: `GET /api/geopoints/nearest`

**Description**: Retrieves the nearest geopoints to a query latitude/longitude within a time range, sorted by proximity.

**Request Parameters**:

- `lat` (double): Latitude of the query point.
- `lon` (double): Longitude of the query point.
- `startTimestamp` (long): Start of the timestamp range.
- `endTimestamp` (long): End of the timestamp range.
- `limit` (int, optional): Maximum number of results to return (default: 10).
- `offset` (int, optional): Results offset for pagination (default: 0).

**Example**:

```bash
curl -X GET "http://localhost:8080/api/geopoints/nearest?lat=37.7749&lon=-122.4194&startTimestamp=1630454400000&endTimestamp=1630540800000&limit=5"
```

The response includes geopoints with their calculated Haversine distance from the query point.

### R-Tree Implementation Details

- **Bounding Boxes**: Each node in the R-tree maintains a bounding box that contains all its child nodes. The bounding
  boxes are updated dynamically during insertion and deletion.
- **Splitting Strategy**: The tree uses a quadratic splitting strategy for balancing nodes when they overflow.
- **Handling Underflows**: Nodes are deleted and their points are reinserted when the number of data points in a node
  drops below the underflow threshold.

### Coordinate Conversion

The `CoordinateService` provides utilities to convert between latitude/longitude and XYZ (ECEF) coordinates. The
distance between points on the Earth's surface is calculated using the Haversine formula.

### Example Data Structure

The `rtree_nodes` table stores the R-tree structure:

```sql
CREATE TABLE rtree_nodes
(
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_id INTEGER,
    minX      REAL,
    maxX      REAL,
    minY      REAL,
    maxY      REAL,
    minZ      REAL,
    maxZ      REAL,
    FOREIGN KEY (parent_id) REFERENCES rtree_nodes (id)
);
```

The `data_points` table stores geopoints:

```sql
CREATE TABLE data_points
(
    id         INTEGER PRIMARY KEY,
    node_id    INTEGER,
    x          REAL,
    y          REAL,
    z          REAL,
    externalId TEXT,
    timestamp  INTEGER,
    FOREIGN KEY (node_id) REFERENCES rtree_nodes (id)
);
```

## Future Improvements

- **Bounding Box Queries**: Support for bounding box queries to further optimize spatial searches.
- **Bulk Inserts**: Batch insertion support to improve performance during data loads.
- **Compression**: Optimizing the storage of bounding boxes and geopoints to reduce the size of the R-tree.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
