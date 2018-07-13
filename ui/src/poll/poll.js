import React from "react";
import Switchboard from "../common/switchboard";
import {Panel} from "react-bootstrap";
import PropTypes from 'prop-types';

import './poll.css';

export default class Poll extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            workDir: this.props.match.params.workDir,
            status: null,
            httpStatus: null,
            opened: false
        };
    }

    componentDidMount() {
        this.pollDeployment();
    }

    pollDeployment() {
        Switchboard.getDeploymentStatus(this.state.workDir).done((data) => {
            this.setState({polling: false});
            const httpStatus = 200;
            const status = data;
            this.setState({httpStatus, status, polling: false}, () => {
                const toPoll = ['DEPLOYED', 'RUNNING'];
                if (httpStatus === 200 && toPoll.includes(status.status)) {
                    setTimeout(() => this.pollDeployment(), this.props.interval);
                }
            });
        }).fail((xhr, msg) => {
            this.setState({httpStatus: xhr.status, status: msg});
        });
        this.setState({polling: true});
    }

    handlePanelClick = () => {
        this.setState({opened: !this.state.opened});
    };

    render() {
        const status = this.state.status;

        let jsonStatus = this.state.httpStatus
            ? <pre>
                {JSON.stringify(status, null, 2)}
            </pre>
            : null;

        let statusTable = this.state.httpStatus === 200
            ? <table className="deployment-status">
                <tbody>
                <tr>
                    <td>status</td>
                    <td>{status.status}</td>
                </tr>
                <tr>
                    <td>service</td>
                    <td>{status.service}</td>
                </tr>
                <tr>
                    <td>work directory</td>
                    <td>{status.workDir}</td>
                </tr>
                <tr>
                    <td>input files</td>
                    <td>{status.files.map(
                        (f, i) => <div key={i}>{f}</div>
                    )}</td>
                </tr>
                {status.status === 'FINISHED'
                    ? <tr>
                        <td>output folder</td>
                        <td>{status.outputDir}</td>
                    </tr>
                    : null
                }
                </tbody>
            </table>
            : null;

        return (
            <div>
                <Panel>
                    <Panel.Heading>
                        <Panel.Title>
                            Deployment status of <code>{this.state.workDir}</code>
                            {this.state.polling ? <i className="fa fa-refresh pull-right"/> : null}
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Body>
                        {statusTable}
                    </Panel.Body>
                </Panel>
                <Panel>
                    <Panel.Heading>
                        <Panel.Title
                            className='clickable'
                            onClick={this.handlePanelClick}
                        >
                            Details
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Body
                        className={this.state.opened ? '' : 'collapse'}
                    >
                        {jsonStatus}
                    </Panel.Body>
                </Panel>
            </div>
        );
    }
}

Poll.propTypes = {
    match: PropTypes.shape({
        params: PropTypes.shape({
            workDir: PropTypes.string.isRequired
        })
    })
};

Poll.defaultTypes = {
    interval: 1000
};