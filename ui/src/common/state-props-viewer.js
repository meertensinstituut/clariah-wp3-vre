import React from "react";

export default class StatePropsViewer extends React.Component {

    render() {
        return (
            <div>
                <div>
                    <p>state:</p>
                    <pre>{JSON.stringify(this.props.state, null, 2)}</pre>
                </div>
                <div>
                    <p>props:</p>
                    <pre>{JSON.stringify(this.props.props, null, 2)}</pre>
                </div>
            </div>
        );
    }
}