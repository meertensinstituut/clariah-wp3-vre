import React from "react";
import Objects from "./objects";
import {Alert, Col, Grid, Row, Tab, Tabs} from "react-bootstrap";

export default class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            title: "VRE",
            subtitle: "A CLARIAH WP3 Virtual Research Environment",
            msg: "Welcome to Clariah's VRE",
        };
    }

    render() {
        return (
            <div>
                <Grid>
                    <Row>
                        <Col xs={12} md={12} className="header">
                            <h1>{this.state.title}</h1>
                            <p>{this.state.subtitle}</p>
                        </Col>
                    </Row>
                    <Row>
                        <Col xs={12} md={12} className="content">
                            <Tabs defaultActiveKey={1} id="primary-tabs">
                                <Tab eventKey={1} title="Home">
                                    <Alert bsStyle="info">{this.state.msg}</Alert>
                                </Tab>
                                <Tab eventKey={2} title="Objects">
                                    <Objects/>
                                </Tab>
                            </Tabs>
                        </Col>
                    </Row>
                </Grid>
            </div>
        );
    }
}
